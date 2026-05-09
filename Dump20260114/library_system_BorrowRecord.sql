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
-- Table structure for table `BorrowRecord`
--

DROP TABLE IF EXISTS `BorrowRecord`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `BorrowRecord` (
  `BorrowID` int NOT NULL AUTO_INCREMENT,
  `StudentID` int NOT NULL,
  `ISBN` varchar(20) NOT NULL,
  `BorrowDate` date NOT NULL,
  `DueDate` date NOT NULL,
  `ReturnDate` date DEFAULT NULL,
  `FineAmount` decimal(8,2) DEFAULT '0.00',
  `Status` enum('Borrowed','Returned','Overdue') DEFAULT 'Borrowed',
  PRIMARY KEY (`BorrowID`),
  KEY `ISBN` (`ISBN`),
  KEY `idx_borrow_student` (`StudentID`),
  KEY `idx_borrow_status` (`Status`),
  CONSTRAINT `borrowrecord_ibfk_1` FOREIGN KEY (`StudentID`) REFERENCES `Student` (`StudentID`) ON DELETE CASCADE,
  CONSTRAINT `borrowrecord_ibfk_2` FOREIGN KEY (`ISBN`) REFERENCES `Book` (`ISBN`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `BorrowRecord`
--

LOCK TABLES `BorrowRecord` WRITE;
/*!40000 ALTER TABLE `BorrowRecord` DISABLE KEYS */;
INSERT INTO `BorrowRecord` VALUES (1,2023380168,'978-0451524935','2026-01-12','2026-01-26',NULL,0.00,'Borrowed'),(2,1001,'978-0451524935','2026-01-13','2026-01-27',NULL,0.00,'Borrowed'),(3,1001,'978-0451524935','2026-01-13','2026-01-27','2026-01-13',0.00,'Returned'),(4,1001,'978-0451524935','2026-01-13','2026-01-27','2026-01-13',0.00,'Returned');
/*!40000 ALTER TABLE `BorrowRecord` ENABLE KEYS */;
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
