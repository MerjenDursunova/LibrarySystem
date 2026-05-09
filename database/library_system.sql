-- Consolidated SQL script from Dump20260114
-- This script recreates the entire schema and data.

USE library_system;

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

--
-- Table structure for table `Category`
--
DROP TABLE IF EXISTS `Category`;
CREATE TABLE `Category` (
  `CategoryID` int NOT NULL AUTO_INCREMENT,
  `Name` varchar(50) NOT NULL,
  PRIMARY KEY (`CategoryID`),
  UNIQUE KEY `Name` (`Name`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

LOCK TABLES `Category` WRITE;
INSERT INTO `Category` VALUES (1,'Computer Science'),(5,'Engineering'),(4,'Fiction'),(2,'Mathematics'),(3,'Physics');
UNLOCK TABLES;

--
-- Table structure for table `Publisher`
--
DROP TABLE IF EXISTS `Publisher`;
CREATE TABLE `Publisher` (
  `PublisherID` int NOT NULL AUTO_INCREMENT,
  `Name` varchar(100) NOT NULL,
  `Address` text,
  PRIMARY KEY (`PublisherID`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

LOCK TABLES `Publisher` WRITE;
INSERT INTO `Publisher` VALUES (1,'Penguin Random House','New York, USA'),(2,'Oxford University Press','Oxford, UK'),(3,'Springer Nature','Berlin, Germany');
UNLOCK TABLES;

--
-- Table structure for table `Student`
--
DROP TABLE IF EXISTS `Student`;
CREATE TABLE `Student` (
  `StudentID` int NOT NULL,
  `Name` varchar(100) NOT NULL,
  `Email` varchar(100) DEFAULT NULL,
  `Phone` varchar(20) DEFAULT NULL,
  `Password` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`StudentID`),
  UNIQUE KEY `Email` (`Email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

LOCK TABLES `Student` WRITE;
INSERT INTO `Student` VALUES (1001,'Alice Johnson','alice@uni.edu','555-0101','703b0a3d6ad75b649a28adde7d83c6251da457549263bc7ff45ec709b0a8448b'),(1002,'Bob Smith','bob@uni.edu','555-0102','703b0a3d6ad75b649a28adde7d83c6251da457549263bc7ff45ec709b0a8448b'),(1003,'Charlie Brown','charlie@uni.edu','555-0103','703b0a3d6ad75b649a28adde7d83c6251da457549263bc7ff45ec709b0a8448b'),(1004,'Diana Ross','diana@uni.edu','555-0104','703b0a3d6ad75b649a28adde7d83c6251da457549263bc7ff45ec709b0a8448b'),(2023380168,' Jenova Anderson','jenova05@gmail.com','555-1234','703b0a3d6ad75b649a28adde7d83c6251da457549263bc7ff45ec709b0a8448b'),(2022350196,'Evangeline Johnson','sweetfwlr@gmail.com',NULL,NULL);
UNLOCK TABLES;

--
-- Table structure for table `Librarian`
--
DROP TABLE IF EXISTS `Librarian`;
CREATE TABLE `Librarian` (
  `LibrarianID` int NOT NULL AUTO_INCREMENT,
  `Name` varchar(100) NOT NULL,
  `Email` varchar(100) DEFAULT NULL,
  `Password` varchar(100) NOT NULL,
  PRIMARY KEY (`LibrarianID`),
  UNIQUE KEY `Email` (`Email`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

LOCK TABLES `Librarian` WRITE;
INSERT INTO `Librarian` VALUES (1,'Admin Librarian','admin@library.edu','240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9'),(2,'John Smith','john@library.edu','ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f'),(3,'Evangeline Johnson','sweetflwr@gmail.com','cfb7745fa74444627af8a79eca4f30c1f2f22ac7ead2cd1664658655229133c2');
UNLOCK TABLES;

--
-- Table structure for table `Book`
--
DROP TABLE IF EXISTS `Book`;
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

LOCK TABLES `Book` WRITE;
INSERT INTO `Book` VALUES ('978-0134685991','Database System Concepts','Abraham Silberschatz',2020,1,1,5,5),('978-0135166307','Modern Operating Systems','Andrew Tanenbaum',2016,1,1,3,3),('978-0198739838','University Physics','Hugh D. Young',2015,2,3,2,2),('978-0321982384','Thomas\' Calculus','George B. Thomas',2015,2,2,3,3),('978-0451524935','1984','George Orwell',1949,1,4,6,4),('978-0470128688','Introduction to Algorithms','Thomas H. Cormen',2009,2,1,4,4),('978-1118290277','Engineering Mechanics','J. L. Meriam',2012,3,5,2,2);
UNLOCK TABLES;

--
-- Table structure for table `BorrowRecord`
--
DROP TABLE IF EXISTS `BorrowRecord`;
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

LOCK TABLES `BorrowRecord` WRITE;
INSERT INTO `BorrowRecord` VALUES (1,2023380168,'978-0451524935','2026-01-12','2026-01-26',NULL,0.00,'Borrowed'),(2,1001,'978-0451524935','2026-01-13','2026-01-27',NULL,0.00,'Borrowed'),(3,1001,'978-0451524935','2026-01-13','2026-01-27','2026-01-13',0.00,'Returned'),(4,1001,'978-0451524935','2026-01-13','2026-01-27','2026-01-13',0.00,'Returned');
UNLOCK TABLES;

--
-- Table structure for table `Reservation`
--
DROP TABLE IF EXISTS `Reservation`;
CREATE TABLE `Reservation` (
  `ReservationID` int NOT NULL AUTO_INCREMENT,
  `StudentID` int NOT NULL,
  `ISBN` varchar(20) NOT NULL,
  `ReserveDate` date NOT NULL,
  `Status` enum('Active','Cancelled','Fulfilled') DEFAULT 'Active',
  PRIMARY KEY (`ReservationID`),
  KEY `StudentID` (`StudentID`),
  KEY `idx_reservation_isbn` (`ISBN`,`Status`),
  CONSTRAINT `reservation_ibfk_1` FOREIGN KEY (`StudentID`) REFERENCES `Student` (`StudentID`) ON DELETE CASCADE,
  CONSTRAINT `reservation_ibfk_2` FOREIGN KEY (`ISBN`) REFERENCES `Book` (`ISBN`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `Reservation`
--
LOCK TABLES `Reservation` WRITE;
UNLOCK TABLES;

--
-- View structure for view `overduebooks`
--
DROP TABLE IF EXISTS `overduebooks`;
/*!50001 DROP VIEW IF EXISTS `overduebooks`*/;
/*!50001 CREATE VIEW `overduebooks` AS select `br`.`BorrowID` AS `BorrowID`,`s`.`Name` AS `StudentName`,`s`.`Email` AS `Email`,`b`.`Title` AS `BookTitle`,`br`.`BorrowDate` AS `BorrowDate`,`br`.`DueDate` AS `DueDate`,(to_days(curdate()) - to_days(`br`.`DueDate`)) AS `DaysOverdue`,`br`.`FineAmount` AS `FineAmount` from ((`BorrowRecord` `br` join `Student` `s` on((`br`.`StudentID` = `s`.`StudentID`))) join `Book` `b` on((`br`.`ISBN` = `b`.`ISBN`))) where ((`br`.`Status` = 'Borrowed') and (curdate() > `br`.`DueDate`)) */;

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
