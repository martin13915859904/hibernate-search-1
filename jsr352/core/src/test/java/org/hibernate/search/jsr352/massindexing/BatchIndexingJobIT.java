/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.StepExecution;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.criteria.CriteriaQuery;

import org.hibernate.criterion.Restrictions;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.jsr352.logging.impl.Log;
import org.hibernate.search.jsr352.massindexing.impl.steps.lucene.StepProgress;
import org.hibernate.search.jsr352.massindexing.test.entity.Company;
import org.hibernate.search.jsr352.massindexing.test.entity.CompanyGroup;
import org.hibernate.search.jsr352.massindexing.test.entity.Person;
import org.hibernate.search.jsr352.massindexing.test.entity.WhoAmI;
import org.hibernate.search.jsr352.test.util.JobTestUtil;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Mincong Huang
 */
public class BatchIndexingJobIT {

	private static final Log log = LoggerFactory.make( Log.class );

	private static final String PERSISTENCE_UNIT_NAME = "h2";
	private static final String SESSION_FACTORY_NAME = "h2-entityManagerFactory";

	private static final int JOB_TIMEOUT_MS = 10_000;

	private static final String MAIN_STEP_NAME = "produceLuceneDoc";

	private JobOperator jobOperator = BatchRuntime.getJobOperator();
	private EntityManagerFactory emf;

	@Before
	public void setup() {
		List<Company> companies = Arrays.asList(
				new Company( "Google" ),
				new Company( "Red Hat" ),
				new Company( "Microsoft" ) );
		List<Person> people = Arrays.asList(
				new Person( "BG", "Bill", "Gates" ),
				new Person( "LT", "Linus", "Torvalds" ),
				new Person( "SJ", "Steven", "Jobs" ) );
		List<WhoAmI> whos = Arrays.asList(
				new WhoAmI( "cid01", "id01", "uid01" ),
				new WhoAmI( "cid02", "id02", "uid02" ),
				new WhoAmI( "cid03", "id03", "uid03" ) );
		EntityManager em = null;

		try {
			emf = Persistence.createEntityManagerFactory( PERSISTENCE_UNIT_NAME );
			em = emf.createEntityManager();
			em.getTransaction().begin();
			for ( Company c : companies ) {
				em.persist( c );
			}
			for ( Person p : people ) {
				em.persist( p );
			}
			for ( WhoAmI w : whos ) {
				em.persist( w );
			}
			em.getTransaction().commit();
		}
		finally {
			try {
				em.close();
			}
			catch (Exception e) {
				log.error( e );
			}
		}
	}

	@After
	public void shutdown() {
		emf.close();
	}

	@Test
	public void simple() throws InterruptedException,
			IOException {
		EntityManager em = emf.createEntityManager();
		FullTextEntityManager ftem = Search.getFullTextEntityManager( em );
		ftem.purgeAll( Person.class );
		ftem.purgeAll( Company.class );
		ftem.purgeAll( WhoAmI.class );
		ftem.flushToIndexes();
		em.close();
		List<Company> companies = JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" );
		List<Person> people = JobTestUtil.findIndexedResults( emf, Person.class, "firstName", "Linus" );
		List<WhoAmI> whos = JobTestUtil.findIndexedResults( emf, WhoAmI.class, "id", "id01" );
		assertEquals( 0, companies.size() );
		assertEquals( 0, people.size() );
		assertEquals( 0, whos.size() );

		long executionId = jobOperator.start(
				MassIndexingJob.NAME,
				MassIndexingJob.parameters()
						.forEntities( Company.class, Person.class, WhoAmI.class )
						.entityManagerFactoryReference( PERSISTENCE_UNIT_NAME )
						.build()
				);
		JobExecution jobExecution = jobOperator.getJobExecution( executionId );
		JobTestUtil.waitForTermination( jobOperator, jobExecution, JOB_TIMEOUT_MS );
		assertCompletion( executionId );
		assertProgress( executionId, Person.class, 3 );
		assertProgress( executionId, Company.class, 3 );
		assertProgress( executionId, WhoAmI.class, 3 );

		companies = JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" );
		people = JobTestUtil.findIndexedResults( emf, Person.class, "firstName", "Linus" );
		whos = JobTestUtil.findIndexedResults( emf, WhoAmI.class, "id", "id01" );
		assertEquals( 1, companies.size() );
		assertEquals( 1, people.size() );
		assertEquals( 1, whos.size() );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2637")
	public void indexedEmbeddedCollection() throws InterruptedException,
			IOException {
		EntityManager em = null;

		try {
			em = emf.createEntityManager();
			em.getTransaction().begin();
			CriteriaQuery<Company> criteria = em.getCriteriaBuilder().createQuery(Company.class);
			criteria.from( Company.class );
			List<Company> companies = em.createQuery( criteria ).getResultList();
			List<CompanyGroup> groups = Arrays.asList(
					new CompanyGroup( "group1", companies.get( 0 ) ),
					new CompanyGroup( "group2", companies.get( 0 ), companies.get( 1 ) ),
					new CompanyGroup( "group3", companies.get( 2 ) ) );
			for ( CompanyGroup group : groups ) {
				em.persist( group );
			}
			em.getTransaction().commit();
		}
		finally {
			try {
				em.close();
			}
			catch (Exception e) {
				log.error( e );
			}
		}

		em = emf.createEntityManager();
		FullTextEntityManager ftem = Search.getFullTextEntityManager( em );
		ftem.purgeAll( CompanyGroup.class );
		ftem.flushToIndexes();
		em.close();
		List<CompanyGroup> groupsContainingGoogle = JobTestUtil.findIndexedResults( emf, CompanyGroup.class, "companies.name", "Google" );
		List<CompanyGroup> groupsContainingRedHat = JobTestUtil.findIndexedResults( emf, CompanyGroup.class, "companies.name", "Red Hat" );
		List<CompanyGroup> groupsContainingMicrosoft = JobTestUtil.findIndexedResults( emf, CompanyGroup.class, "companies.name", "Microsoft" );
		assertEquals( 0, groupsContainingGoogle.size() );

		long executionId = jobOperator.start(
				MassIndexingJob.NAME,
				MassIndexingJob.parameters()
						.forEntities( CompanyGroup.class )
						.entityManagerFactoryReference( PERSISTENCE_UNIT_NAME )
						.build()
				);
		JobExecution jobExecution = jobOperator.getJobExecution( executionId );
		JobTestUtil.waitForTermination( jobOperator, jobExecution, JOB_TIMEOUT_MS );
		assertCompletion( executionId );
		assertProgress( executionId, CompanyGroup.class, 3 );

		groupsContainingGoogle = JobTestUtil.findIndexedResults( emf, CompanyGroup.class, "companies.name", "Google" );
		groupsContainingRedHat = JobTestUtil.findIndexedResults( emf, CompanyGroup.class, "companies.name", "Red Hat" );
		groupsContainingMicrosoft = JobTestUtil.findIndexedResults( emf, CompanyGroup.class, "companies.name", "Microsoft" );
		assertEquals( 2, groupsContainingGoogle.size() );
		assertEquals( 1, groupsContainingRedHat.size() );
		assertEquals( 1, groupsContainingMicrosoft.size() );
	}

	@Test
	public void entityManagerFactoryNamespace_persistenceUnitName() throws Exception {
		EntityManager em = emf.createEntityManager();
		FullTextEntityManager ftem = Search.getFullTextEntityManager( em );
		ftem.purgeAll( Company.class );
		List<Company> companies = JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" );
		assertEquals( 0, companies.size() );

		long executionId = jobOperator.start(
				MassIndexingJob.NAME,
				MassIndexingJob.parameters()
						.forEntity( Company.class )
						.entityManagerFactoryNamespace( "persistence-unit-name" )
						.entityManagerFactoryReference( PERSISTENCE_UNIT_NAME )
						.build()
				);
		JobExecution jobExecution = jobOperator.getJobExecution( executionId );
		JobTestUtil.waitForTermination( jobOperator, jobExecution, JOB_TIMEOUT_MS );

		companies = JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" );
		assertEquals( 1, companies.size() );
	}

	@Test
	public void entityManagerFactoryNamespace_sessionFactoryName() throws Exception {
		EntityManager em = emf.createEntityManager();
		FullTextEntityManager ftem = Search.getFullTextEntityManager( em );
		ftem.purgeAll( Company.class );
		List<Company> companies = JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" );
		assertEquals( 0, companies.size() );

		long executionId = jobOperator.start(
				MassIndexingJob.NAME,
				MassIndexingJob.parameters()
						.forEntity( Company.class )
						.entityManagerFactoryNamespace( "session-factory-name" )
						.entityManagerFactoryReference( SESSION_FACTORY_NAME )
						.build()
				);
		JobExecution jobExecution = jobOperator.getJobExecution( executionId );
		JobTestUtil.waitForTermination( jobOperator, jobExecution, JOB_TIMEOUT_MS );

		companies = JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" );
		assertEquals( 1, companies.size() );
	}

	@Test
	public void criteria() throws InterruptedException,
			IOException {

		// purge all before start
		// TODO Can the creation of a new EM and FTEM be avoided?
		EntityManager em = emf.createEntityManager();
		FullTextEntityManager ftem = Search.getFullTextEntityManager( em );
		ftem.purgeAll( Person.class );
		ftem.purgeAll( Company.class );
		ftem.flushToIndexes();
		em.close();

		// searches before mass index,
		// expected no results for each search
		assertEquals( 0, JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" ).size() );
		assertEquals( 0, JobTestUtil.findIndexedResults( emf, Company.class, "name", "Red Hat" ).size() );
		assertEquals( 0, JobTestUtil.findIndexedResults( emf, Company.class, "name", "Microsoft" ).size() );

		long executionId = jobOperator.start(
				MassIndexingJob.NAME,
				MassIndexingJob.parameters()
						.forEntity( Company.class )
						.restrictedBy( Restrictions.in( "name", "Google", "Red Hat" ) )
						.entityManagerFactoryReference( PERSISTENCE_UNIT_NAME )
						.build()
				);
		JobExecution jobExecution = jobOperator.getJobExecution( executionId );
		JobTestUtil.waitForTermination( jobOperator, jobExecution, JOB_TIMEOUT_MS );

		assertEquals( 1, JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" ).size() );
		assertEquals( 1, JobTestUtil.findIndexedResults( emf, Company.class, "name", "Red Hat" ).size() );
		assertEquals( 0, JobTestUtil.findIndexedResults( emf, Company.class, "name", "Microsoft" ).size() );
	}

	@Test
	public void hql() throws InterruptedException,
			IOException {

		// purge all before start
		// TODO Can the creation of a new EM and FTEM be avoided?
		EntityManager em = emf.createEntityManager();
		FullTextEntityManager ftem = Search.getFullTextEntityManager( em );
		ftem.purgeAll( Person.class );
		ftem.purgeAll( Company.class );
		ftem.flushToIndexes();
		em.close();

		// searches before mass index,
		// expected no results for each search
		assertEquals( 0, JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" ).size() );
		assertEquals( 0, JobTestUtil.findIndexedResults( emf, Company.class, "name", "Red Hat" ).size() );
		assertEquals( 0, JobTestUtil.findIndexedResults( emf, Company.class, "name", "Microsoft" ).size() );

		long executionId = jobOperator.start(
				MassIndexingJob.NAME,
				MassIndexingJob.parameters()
						.forEntity( Company.class )
						.restrictedBy( "select c from Company c where c.name in ( 'Google', 'Red Hat' )" )
						.entityManagerFactoryReference( PERSISTENCE_UNIT_NAME )
						.build()
				);
		JobExecution jobExecution = jobOperator.getJobExecution( executionId );
		JobTestUtil.waitForTermination( jobOperator, jobExecution, JOB_TIMEOUT_MS );

		assertEquals( 1, JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" ).size() );
		assertEquals( 1, JobTestUtil.findIndexedResults( emf, Company.class, "name", "Red Hat" ).size() );
		assertEquals( 0, JobTestUtil.findIndexedResults( emf, Company.class, "name", "Microsoft" ).size() );
	}

	@Test
	public void partitioned() throws InterruptedException,
			IOException {
		EntityManager em = emf.createEntityManager();
		FullTextEntityManager ftem = Search.getFullTextEntityManager( em );
		ftem.purgeAll( Person.class );
		ftem.purgeAll( Company.class );
		ftem.purgeAll( WhoAmI.class );
		ftem.flushToIndexes();
		em.close();
		List<Company> companies = JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" );
		List<Person> people = JobTestUtil.findIndexedResults( emf, Person.class, "firstName", "Linus" );
		List<WhoAmI> whos = JobTestUtil.findIndexedResults( emf, WhoAmI.class, "id", "id01" );
		assertEquals( 0, companies.size() );
		assertEquals( 0, people.size() );
		assertEquals( 0, whos.size() );

		long executionId = jobOperator.start(
				MassIndexingJob.NAME,
				MassIndexingJob.parameters()
						.forEntities( Company.class, Person.class, WhoAmI.class )
						.entityManagerFactoryReference( PERSISTENCE_UNIT_NAME )
						.rowsPerPartition( 2 )
						.checkpointInterval( 1 )
						.build()
				);
		JobExecution jobExecution = jobOperator.getJobExecution( executionId );
		JobTestUtil.waitForTermination( jobOperator, jobExecution, JOB_TIMEOUT_MS );
		assertCompletion( executionId );
		assertProgress( executionId, Person.class, 3 );
		assertProgress( executionId, Company.class, 3 );
		assertProgress( executionId, WhoAmI.class, 3 );

		StepProgress progress = getMainStepProgress( executionId );
		Map<Integer, Long> partitionProgress = progress.getPartitionProgress();
		assertThat( partitionProgress )
				.as( "Entities processed per partition" )
				.hasSize( 3 * 2 )
				.includes(
						// Company
						entry( 0, 2L ), // Partition 1
						entry( 1, 1L ), // Partition 2
						// Person
						entry( 2, 2L ), // Partition 1
						entry( 3, 1L ), // Partition 2
						// WhoAmI
						entry( 4, 2L ), // Partition 1
						entry( 5, 1L ) // Partition 2
				);

		companies = JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" );
		people = JobTestUtil.findIndexedResults( emf, Person.class, "firstName", "Linus" );
		whos = JobTestUtil.findIndexedResults( emf, WhoAmI.class, "id", "id01" );
		assertEquals( 1, companies.size() );
		assertEquals( 1, people.size() );
		assertEquals( 1, whos.size() );
	}

	private void assertCompletion(long executionId) {
		List<StepExecution> stepExecutions = jobOperator.getStepExecutions( executionId );
		for ( StepExecution stepExecution : stepExecutions ) {
			BatchStatus batchStatus = stepExecution.getBatchStatus();
			log.infof( "step %s executed.", stepExecution.getStepName() );
			assertEquals( BatchStatus.COMPLETED, batchStatus );
		}
	}

	private void assertProgress(long executionId, Class<?> entityType, int progressValue) {
		/*
		 * We cannot check the metrics, which in JBatch are set to 0
		 * for partitioned steps (the metrics are handled separately for
		 * each partition).
		 * Thus we check our own object.
		 */
		StepProgress progress = getMainStepProgress( executionId );
		assertEquals( Long.valueOf( progressValue ), progress.getEntityProgress().get( entityType.getName() ) );
	}

	private StepProgress getMainStepProgress(long executionId) {
		List<StepExecution> stepExecutions = jobOperator.getStepExecutions( executionId );
		for ( StepExecution stepExecution : stepExecutions ) {
			switch ( stepExecution.getStepName() ) {
				case MAIN_STEP_NAME:
					return (StepProgress) stepExecution.getPersistentUserData();
				default:
					break;
			}
		}
		throw new AssertionFailure( "Missing step progress for step '" + MAIN_STEP_NAME + "'" );
	}

}