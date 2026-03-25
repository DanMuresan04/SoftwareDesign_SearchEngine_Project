# System Architecture: Local Search Engine

## 1. System Context (Level 1)
The system context is the top level that represents the entire system, providing a high-level view of how it integrates into the operational environment.
* **End User:** A person operating a personal workstation and interacting with the application interface.
* **Search Engine (Main System):** The central Java application I am developing within IntelliJ IDEA, whose responsibility is to build a local file search system. It indexes files on the device, including documents, media, and binaries, in order to provide a fast and responsive experience where results appear as the user types.

## 2. Containers (Level 2)
The system context comprises a number of containers that represent deployable units.
* **Client Application (Graphical Interface):** The interactive module, built using a Java GUI framework like JavaFX or Swing, designed to retrieve keywords and present results, including mandatory file previews (e.g. the first 3 lines of a document).
* **Collection and Indexing Service:** A background Java process,  that traverses local content, filters out unwanted data and stores it in a database. In this first iteration, I am focusing exclusively on searching for content within text files.
* **Database Management System:** The preferred storage solution, ideally using an embedded SQLite database via a JDBC driver (`org.sqlite.JDBC`) or H2 Database, its schema determining exactly how the data will be processed at the time of query.

## 3. Components (Level 3)
Each container contains a number of components, which are the major building blocks in the code.
* **File Crawler:** A vital component utilizing NIO.2 APIs like `java.nio.file.Files.walkFileTree` to recursively traverse the file system. I am designing it to gracefully handle exceptions via Java `try-catch` blocks, avoiding crashes in situations such as an `AccessDeniedException`, symbolic link loops, or database connection timeouts.
* **Data and Metadata Extractor:** The entity that operates under the principle that every bit of information is important, ensuring that all available metadata (extensions, tags, `BasicFileAttributes` timestamps) is extracted properly and preserved for future use cases.
* **SQL Query Orchestrator:** The component, designed using raw JDBC `PreparedStatement` objects or an ORM like Hibernate/JPA, that analyzes what is returned by the query, transforming the terms into appropriate SQL commands to ensure that single-word and multi-word searches work.

## 4. Code (Level 4)
Finally, each component contains a number of Java classes, interfaces, or `record` types that contain the core logic.
* **`IndexingConfigurator` and `ProgressReporter`:** Java classes (or `record` types) dedicated to runtime configuration (ignore rules, root directory, report format, often loaded using `java.util.Properties`). They keep track of the progress and generate a detailed report when the process is complete.
* **`IncrementalIndexManager`:** A class essential for achieving excellent performance, responsible for performing incremental indexing by detecting file modifications via `lastModifiedTime()` attributes or `java.security.MessageDigest` hashes, and updating only the records in question with database upserts, avoiding a total database rebuild.
