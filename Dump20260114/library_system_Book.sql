-- MySQL dump 10.13  Distrib 8.0.44, for macos15 (arm64)
--
-- Host: localhost    Database: library_system
-- ------------------------------------------------------
-- Server version	9.5.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
SET @MYSQLDUMP_TEMP_LOG_BIN = @@SESSION.SQL_LOG_BIN;
SET @@SESSION.SQL_LOG_BIN= 0;

--
-- GTID state at the beginning of the backup 
--

SET @@GLOBAL.GTID_PURGED=/*!80000 '+'*/ '4397fa18-e5a8-11f0-a52f-d2bf4621d2bc:1-102383';

--
-- Table structure for table `Book`
--

DROP TABLE IF EXISTS `Book`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Book` (
  `ISBN` varchar(20) NOT NULL,
  `Title` varchar(200) NOT NULL,
  `Author` varchar(100) NOT NULL,
  `Year` int DEFAULT NULL,
  `PublisherID` int DEFAULT NULL,
  `CategoryID` int DEFAULT NULL,
  `Total_Copies` int DEFAULT '1',
  `Available_Copies` int DEFAULT '1',
  PRIMARY KEY (`ISBN`),
  KEY `PublisherID` (`PublisherID`),
  KEY `CategoryID` (`CategoryID`),
  KEY `idx_book_title` (`Title`),
  KEY `idx_book_author` (`Author`),
  CONSTRAINT `book_ibfk_1` FOREIGN KEY (`PublisherID`) REFERENCES `Publisher` (`PublisherID`),
  CONSTRAINT `book_ibfk_2` FOREIGN KEY (`CategoryID`) REFERENCES `Category` (`CategoryID`),
  CONSTRAINT `book_chk_1` CHECK ((`Available_Copies` <= `Total_Copies`)),
  CONSTRAINT `book_chk_2` CHECK ((`Available_Copies` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Book`
--

LOCK TABLES `Book` WRITE;
/*!40000 ALTER TABLE `Book` DISABLE KEYS */;
INSERT INTO `Book` VALUES ('978-0134685991','Database System Concepts','Abraham Silberschatz',2020,1,1,5,5),('978-0135166307','Modern Operating Systems','Andrew Tanenbaum',2016,1,1,3,3),('978-0198739838','University Physics','Hugh D. Young',2015,2,3,2,2),('978-0321982384','Thomas\' Calculus','George B. Thomas',2015,2,2,3,3),('978-0451524935','1984','George Orwell',1949,1,4,6,4),('978-0470128688','Introduction to Algorithms','Thomas H. Cormen',2009,2,1,4,4),('978-1118290277','Engineering Mechanics','J. L. Meriam',2012,3,5,2,2);
/*!40000 ALTER TABLE `Book` ENABLE KEYS */;
UNLOCK TABLES;
SET @@SESSION.SQL_LOG_BIN = @MYSQLDUMP_TEMP_LOG_BIN;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-01-14 17:36:37
