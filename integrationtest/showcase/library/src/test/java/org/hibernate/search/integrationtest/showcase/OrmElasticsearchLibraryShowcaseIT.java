/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.search.backend.elasticsearch.impl.ElasticsearchBackendFactory;
import org.hibernate.search.engine.backend.spatial.GeoPoint;
import org.hibernate.search.engine.backend.spatial.ImmutableGeoPoint;
import org.hibernate.search.mapper.orm.cfg.SearchOrmSettings;
import org.hibernate.search.integrationtest.showcase.library.dao.DaoFactory;
import org.hibernate.search.integrationtest.showcase.library.dao.DocumentDao;
import org.hibernate.search.integrationtest.showcase.library.dao.syntax.fluid.FluidAndObjectSyntaxDaoFactory;
import org.hibernate.search.integrationtest.showcase.library.dao.syntax.lambda.LambdaSyntaxDaoFactory;
import org.hibernate.search.integrationtest.showcase.library.dao.LibraryDao;
import org.hibernate.search.integrationtest.showcase.library.model.Book;
import org.hibernate.search.integrationtest.showcase.library.model.BookCopy;
import org.hibernate.search.integrationtest.showcase.library.model.BookMedium;
import org.hibernate.search.integrationtest.showcase.library.model.Document;
import org.hibernate.search.integrationtest.showcase.library.model.DocumentCopy;
import org.hibernate.search.integrationtest.showcase.library.model.ISBN;
import org.hibernate.search.integrationtest.showcase.library.model.Library;
import org.hibernate.search.integrationtest.showcase.library.model.LibraryService;
import org.hibernate.search.integrationtest.showcase.library.model.Video;
import org.hibernate.search.integrationtest.showcase.library.model.VideoCopy;
import org.hibernate.search.integrationtest.showcase.library.model.VideoMedium;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.Action;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.search.integrationtest.util.orm.OrmUtils.withinSession;
import static org.hibernate.search.integrationtest.util.orm.OrmUtils.withinTransaction;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

@RunWith(Parameterized.class)
public class OrmElasticsearchLibraryShowcaseIT {

	@Parameterized.Parameters(name = "{0}")
	public static List<DaoFactory> daoFactories() {
		return Arrays.asList( new FluidAndObjectSyntaxDaoFactory(), new LambdaSyntaxDaoFactory() );
	}

	private static final String PREFIX = SearchOrmSettings.PREFIX;

	// Document IDs
	private static final int CALLIGRAPHY_ID = 1;
	private static final int JAVA_DANCING_ID = 2;
	private static final int INDONESIAN_ECONOMY_ID = 3;
	private static final int JAVA_FOR_DUMMIES_ID = 4;
	private static final int ART_OF_COMPUTER_PROG_ID = 5;
	private static final int THESAURUS_OF_LANGUAGES_ID = 6;
	private static final int LIVING_ON_ISLAND_ID = 7;

	// Library IDs
	private static final int CITY_CENTER_ID = 1;
	private static final int SUBURBAN_1_ID = 2;
	private static final int SUBURBAN_2_ID = 3;
	private static final int UNIVERSITY_ID = 4;

	private SessionFactory sessionFactory;

	private DaoFactory daoFactory;

	public OrmElasticsearchLibraryShowcaseIT(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Before
	public void setup() {
		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder()
				.applySetting( PREFIX + "backend.elasticsearchBackend_1.type", ElasticsearchBackendFactory.class.getName() )
				.applySetting( PREFIX + "index.default.backend", "elasticsearchBackend_1" )
				.applySetting( org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO, Action.CREATE_DROP );

		ServiceRegistry serviceRegistry = registryBuilder.build();

		MetadataSources ms = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( Document.class )
				.addAnnotatedClass( Book.class )
				.addAnnotatedClass( Video.class )
				.addAnnotatedClass( Library.class )
				.addAnnotatedClass( DocumentCopy.class )
				.addAnnotatedClass( BookCopy.class )
				.addAnnotatedClass( VideoCopy.class );

		Metadata metadata = ms.buildMetadata();

		final SessionFactoryBuilder sfb = metadata.getSessionFactoryBuilder();
		this.sessionFactory = sfb.build();
	}

	@After
	public void cleanup() {
		if ( sessionFactory != null ) {
			sessionFactory.close();
		}
	}

	@Test
	public void search() {
		withinTransaction( sessionFactory, this::initData );

		withinSession( sessionFactory, session -> {
			LibraryDao dao = daoFactory.createLibraryDao( session );

			List<Library> libraries = dao.search( "library", 0, 10 );
			assertThat( libraries ).containsExactly(
					session.get( Library.class, CITY_CENTER_ID ),
					session.get( Library.class, UNIVERSITY_ID ), // Bumped to this position because of its collection size
					session.get( Library.class, SUBURBAN_1_ID ),
					session.get( Library.class, SUBURBAN_2_ID )
			);
			libraries = dao.search( "library", 1, 2 );
			assertThat( libraries ).containsExactly(
					session.get( Library.class, UNIVERSITY_ID ),
					session.get( Library.class, SUBURBAN_1_ID )
			);
			libraries = dao.search( "sUburban", 0, 10 );
			assertThat( libraries ).containsExactly(
					session.get( Library.class, SUBURBAN_1_ID ),
					session.get( Library.class, SUBURBAN_2_ID )
			);
			// TODO introduce an AND operator in the match query to make this match SUBURBAN_1_ID only
			libraries = dao.search( "Suburban 1", 0, 10 );
			assertThat( libraries ).containsExactly(
					session.get( Library.class, SUBURBAN_1_ID ),
					session.get( Library.class, SUBURBAN_2_ID )
			);
			libraries = dao.search( "city center", 0, 10 );
			assertThat( libraries ).containsExactly(
					session.get( Library.class, CITY_CENTER_ID )
			);
		} );
	}

	@Test
	public void search_single() {
		withinTransaction( sessionFactory, this::initData );

		withinSession( sessionFactory, session -> {
			DocumentDao dao = daoFactory.createDocumentDao( session );

			Optional<Book> book = dao.getByIsbn( "978-0-00-000001-1" );
			assertTrue( book.isPresent() );
			assertThat( book.get() ).isEqualTo( session.get( Book.class, CALLIGRAPHY_ID ) );

			book = dao.getByIsbn( "978-0-00-000005-5" );
			assertTrue( book.isPresent() );
			assertThat( book.get() ).isEqualTo( session.get( Book.class, ART_OF_COMPUTER_PROG_ID ) );

			book = dao.getByIsbn( "978-0-00-000005-1" );
			assertFalse( book.isPresent() );
		} );
	}

	@Test
	public void searchAroundMe_spatial() {
		// TODO spatial queries
		assumeTrue( "Spatial queries not implemented yet", false );

		DocumentDao dao = daoFactory.createDocumentDao( sessionFactory.createEntityManager() );

		withinTransaction( sessionFactory, this::initData );

		withinSession( sessionFactory, session -> {
			GeoPoint myLocation = new ImmutableGeoPoint( 42.0, 0.5 );

			List<Document<?>> documents = dao.searchAroundMe(
					null, null,
					myLocation, 20.0,
					null,
					0, 10
			);
			// Should only include content from university
			assertThat( documents ).containsExactly(
					session.get( Book.class, INDONESIAN_ECONOMY_ID ),
					session.get( Book.class, JAVA_FOR_DUMMIES_ID ),
					session.get( Book.class, ART_OF_COMPUTER_PROG_ID ),
					session.get( Book.class, THESAURUS_OF_LANGUAGES_ID )
			);

			documents = dao.searchAroundMe(
					null, null,
					myLocation, 40.0,
					null,
					0, 10
			);
			// Should only include content from suburb1 or university
			assertThat( documents ).containsExactly(
					session.get( Book.class, CALLIGRAPHY_ID ),
					session.get( Book.class, INDONESIAN_ECONOMY_ID ),
					session.get( Book.class, JAVA_FOR_DUMMIES_ID ),
					session.get( Book.class, ART_OF_COMPUTER_PROG_ID ),
					session.get( Book.class, THESAURUS_OF_LANGUAGES_ID )
			);

			documents = dao.searchAroundMe(
					"calligraphy", null,
					myLocation, 40.0,
					null,
					0, 10
			);
			// Should only include content from suburb1 or university with "calligraphy" in it
			assertThat( documents ).containsExactly(
					session.get( Book.class, CALLIGRAPHY_ID )
			);

			myLocation = new ImmutableGeoPoint( 42.0, 0.75 );
			documents = dao.searchAroundMe(
					null, null,
					myLocation, 40.0,
					null,
					0, 10
			);
			// Should only include content from university
			assertThat( documents ).containsExactly(
					session.get( Book.class, INDONESIAN_ECONOMY_ID ),
					session.get( Book.class, JAVA_FOR_DUMMIES_ID ),
					session.get( Book.class, ART_OF_COMPUTER_PROG_ID ),
					session.get( Book.class, THESAURUS_OF_LANGUAGES_ID )
			);
		} );
	}

	@Test
	public void searchAroundMe_nested() {
		DocumentDao dao = daoFactory.createDocumentDao( sessionFactory.createEntityManager() );

		withinTransaction( sessionFactory, this::initData );

		withinSession( sessionFactory, session -> {
			List<Document<?>> documents = dao.searchAroundMe(
					"java", null,
					null, null,
					Collections.singletonList( LibraryService.DISABLED_ACCESS ),
					0, 10
			);
			assertThat( documents ).containsOnly(
					session.get( Book.class, JAVA_FOR_DUMMIES_ID ),
					session.get( Video.class, JAVA_DANCING_ID ),
					session.get( Book.class, INDONESIAN_ECONOMY_ID )
			);

			documents = dao.searchAroundMe(
					"java", null,
					null, null,
					Collections.singletonList( LibraryService.READING_ROOMS ),
					0, 10
			);
			assertThat( documents ).containsOnly(
					session.get( Book.class, JAVA_FOR_DUMMIES_ID ),
					session.get( Video.class, JAVA_DANCING_ID ),
					session.get( Book.class, INDONESIAN_ECONOMY_ID )
			);

			documents = dao.searchAroundMe(
					"java", null,
					null, null,
					Arrays.asList( LibraryService.DISABLED_ACCESS, LibraryService.READING_ROOMS ),
					0, 10
			);
			/*
			 * In particular, should not match the document "indonesianEconomy",
			 * which is present in a library with disabled access and in a library with reading rooms,
			 * but not in a library with both.
			 */
			assertThat( documents ).containsOnly(
					session.get( Book.class, JAVA_FOR_DUMMIES_ID ),
					session.get( Video.class, JAVA_DANCING_ID )
			);
		} );
	}

	@Test
	public void searchAroundMe_searchBridge() {
		DocumentDao dao = daoFactory.createDocumentDao( sessionFactory.createEntityManager() );

		withinTransaction( sessionFactory, this::initData );

		withinSession( sessionFactory, session -> {
			List<Document<?>> documents = dao.searchAroundMe(
					null, "java",
					null, null,
					null,
					0, 10
			);
			assertThat( documents ).containsOnly(
					session.get( Video.class, JAVA_DANCING_ID ),
					session.get( Book.class, JAVA_FOR_DUMMIES_ID ),
					session.get( Book.class, INDONESIAN_ECONOMY_ID ),
					session.get( Video.class, LIVING_ON_ISLAND_ID )
			);

			documents = dao.searchAroundMe(
					null, "programming",
					null, null,
					null,
					0, 10
			);
			assertThat( documents ).containsOnly(
					session.get( Book.class, JAVA_FOR_DUMMIES_ID ),
					session.get( Book.class, ART_OF_COMPUTER_PROG_ID )
			);

			documents = dao.searchAroundMe(
					null, "java,programming",
					null, null,
					null,
					0, 10
			);
			assertThat( documents ).containsOnly(
					session.get( Book.class, JAVA_FOR_DUMMIES_ID )
			);
		} );
	}

	@Test
	public void aggregation() {
		// TODO aggregation
		assumeTrue( "Aggregation not implemented yet", false );
	}

	private void initData(Session session) {
		LibraryDao libraryDao = daoFactory.createLibraryDao( session );
		DocumentDao documentDao = daoFactory.createDocumentDao( session );

		Book calligraphy = documentDao.createBook(
				CALLIGRAPHY_ID,
				new ISBN( "978-0-00-000001-1" ),
				"Calligraphy for Dummies",
				"Learn to write artfully in ten lessons",
				"calligraphy,art"
		);

		Video javaDancing = documentDao.createVideo(
				JAVA_DANCING_ID,
				"Java le dire à tout le monde",
				"A brief history of Java dancing in Paris during the early 20th century",
				"java,dancing,history"
		);

		Book indonesianEconomy = documentDao.createBook(
				INDONESIAN_ECONOMY_ID,
				new ISBN( "978-0-00-000003-3" ),
				"Comparative Study of the Economy of Java and other Indonesian Islands",
				"Comparative study of the late 20th century economy of the main islands of Indonesia"
						+ " with accurate projections over the next ten centuries",
				"geography,economy,java,sumatra,borneo,sulawesi"
		);

		Book javaForDummies = documentDao.createBook(
				JAVA_FOR_DUMMIES_ID,
				new ISBN( "978-0-00-000004-4" ),
				"Java for Dummies",
				"Learning the Java programming language in ten lessons",
				"programming,language,java"
		);

		Book artOfComputerProg = documentDao.createBook(
				ART_OF_COMPUTER_PROG_ID,
				new ISBN( "978-0-00-000005-5" ),
				"The Art of Computer Programming",
				"Quick review of basic computer programming principles in 965 chapters",
				"programming"
		);

		Book thesaurusOfLanguages = documentDao.createBook(
				THESAURUS_OF_LANGUAGES_ID,
				new ISBN( "978-0-00-000006-6" ),
				"Thesaurus of Indo-European Languages",
				"An entertaining list of about three thousand languages, most of which are long dead",
				"geography,language"
		);

		Video livingOnIsland = documentDao.createVideo(
				LIVING_ON_ISLAND_ID,
				"Living in an Island, Episode 3: Indonesia",
				"A journey across Indonesia's smallest islands depicting how island way of life differs from mainland living",
				"geography,java,sumatra,borneo,sulawesi"
		);

		// City center library
		Library cityCenterLibrary = libraryDao.create(
				CITY_CENTER_ID,
				"City Center Library",
				12400,
				42.0, 0.0,
				LibraryService.READING_ROOMS,
				LibraryService.HARDCOPY_LOAN
		);
		// Content: every document, but no dematerialized copy
		documentDao.createCopy( cityCenterLibrary, calligraphy, BookMedium.HARDCOPY );
		documentDao.createCopy( cityCenterLibrary, javaDancing, VideoMedium.DVD );
		documentDao.createCopy( cityCenterLibrary, indonesianEconomy, BookMedium.HARDCOPY );
		documentDao.createCopy( cityCenterLibrary, javaForDummies, BookMedium.HARDCOPY );
		documentDao.createCopy( cityCenterLibrary, artOfComputerProg, BookMedium.HARDCOPY );
		documentDao.createCopy( cityCenterLibrary, thesaurusOfLanguages, BookMedium.HARDCOPY );
		documentDao.createCopy( cityCenterLibrary, livingOnIsland, VideoMedium.BLURAY );

		// Suburban library 1
		Library suburbanLibrary1 = libraryDao.create(
				SUBURBAN_1_ID,
				"Suburban Library 1",
				800,
				42.0, 0.25,
				LibraryService.DISABLED_ACCESS,
				LibraryService.HARDCOPY_LOAN
		);
		// Content: no video document
		documentDao.createCopy( suburbanLibrary1, calligraphy, BookMedium.HARDCOPY );
		documentDao.createCopy( suburbanLibrary1, indonesianEconomy, BookMedium.HARDCOPY );
		documentDao.createCopy( suburbanLibrary1, javaForDummies, BookMedium.HARDCOPY );
		documentDao.createCopy( suburbanLibrary1, artOfComputerProg, BookMedium.HARDCOPY );
		documentDao.createCopy( suburbanLibrary1, thesaurusOfLanguages, BookMedium.HARDCOPY );

		// Suburban library 2
		Library suburbanLibrary2 = libraryDao.create(
				SUBURBAN_2_ID,
				"Suburban Library 2",
				800, // Same as the other suburban library
				42.0, -0.25,
				LibraryService.DISABLED_ACCESS, LibraryService.READING_ROOMS,
				LibraryService.HARDCOPY_LOAN
		);
		// Content: no academic document, offers dematerialized copies
		documentDao.createCopy( suburbanLibrary2, calligraphy, BookMedium.HARDCOPY );
		documentDao.createCopy( suburbanLibrary2, calligraphy, BookMedium.DEMATERIALIZED );
		documentDao.createCopy( suburbanLibrary2, javaDancing, VideoMedium.DVD );
		documentDao.createCopy( suburbanLibrary2, javaDancing, VideoMedium.DEMATERIALIZED );
		documentDao.createCopy( suburbanLibrary2, javaForDummies, BookMedium.HARDCOPY );
		documentDao.createCopy( suburbanLibrary2, javaForDummies, BookMedium.DEMATERIALIZED );
		documentDao.createCopy( suburbanLibrary2, livingOnIsland, VideoMedium.BLURAY );
		documentDao.createCopy( suburbanLibrary2, livingOnIsland, VideoMedium.DEMATERIALIZED );

		// University library
		Library universityLibrary = libraryDao.create(
				UNIVERSITY_ID,
				"University Library",
				9000,
				42.0, 0.5,
				LibraryService.READING_ROOMS,
				LibraryService.HARDCOPY_LOAN, LibraryService.DEMATERIALIZED_LOAN
		);
		// Content: only academic and learning documents
		documentDao.createCopy( universityLibrary, indonesianEconomy, BookMedium.HARDCOPY );
		documentDao.createCopy( universityLibrary, javaForDummies, BookMedium.HARDCOPY );
		documentDao.createCopy( universityLibrary, artOfComputerProg, BookMedium.HARDCOPY );
		documentDao.createCopy( universityLibrary, thesaurusOfLanguages, BookMedium.HARDCOPY );
	}
}