-- MySQL dump 10.13  Distrib 8.1.0, for Win64 (x86_64)
--
-- Host: localhost    Database: tracker
-- ------------------------------------------------------
-- Server version	8.1.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `bill_budget`
--

DROP TABLE IF EXISTS `bill_budget`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bill_budget` (
  `bill_budget_id` int NOT NULL AUTO_INCREMENT,
  `bill_budget_code` varchar(30) NOT NULL,
  `bill_code` varchar(15) NOT NULL,
  `provider_id` varchar(30) NOT NULL DEFAULT '1',
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `bill_mins` int NOT NULL,
  `bill_mins_remaining` int DEFAULT '0',
  PRIMARY KEY (`bill_budget_id`),
  UNIQUE KEY `uk_bill_budget_provider_code` (`provider_id`,`bill_budget_code`,`bill_code`),
  KEY `idx_bill_budget_provider_bill_code` (`provider_id`,`bill_code`)
) ENGINE=MyISAM AUTO_INCREMENT=28 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `bill_code`
--

DROP TABLE IF EXISTS `bill_code`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bill_code` (
  `bill_code` varchar(15) NOT NULL,
  `bill_label` varchar(150) DEFAULT NULL,
  `billable` varchar(1) NOT NULL,
  `visible` varchar(1) NOT NULL,
  `client_bill_code` varchar(30) DEFAULT NULL,
  `client_bill_description` varchar(120) DEFAULT NULL,
  `provider_id` varchar(30) NOT NULL DEFAULT '1',
  `estimate_min` int NOT NULL DEFAULT '0',
  `bill_rate` int NOT NULL DEFAULT '0',
  `bill_round` int NOT NULL DEFAULT '1',
  PRIMARY KEY (`provider_id`,`bill_code`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `bill_day`
--

DROP TABLE IF EXISTS `bill_day`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bill_day` (
  `bill_day_id` int NOT NULL AUTO_INCREMENT,
  `bill_code` varchar(15) NOT NULL,
  `provider_id` varchar(30) NOT NULL DEFAULT '1',
  `bill_date` date NOT NULL,
  `bill_mins` int NOT NULL,
  `bill_budget_id` int DEFAULT NULL,
  `bill_month_id` int DEFAULT NULL,
  `bill_mins_budget` int DEFAULT '0',
  PRIMARY KEY (`bill_day_id`),
  UNIQUE KEY `uk_bill_day_provider_code_date` (`provider_id`,`bill_code`,`bill_date`),
  KEY `idx_bill_day_provider_bill_code` (`provider_id`,`bill_code`)
) ENGINE=MyISAM AUTO_INCREMENT=6122 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `bill_entry`
--

DROP TABLE IF EXISTS `bill_entry`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bill_entry` (
  `bill_id` int NOT NULL AUTO_INCREMENT,
  `project_id` int NOT NULL,
  `category_code` varchar(15) DEFAULT NULL,
  `username` varchar(30) NOT NULL,
  `web_user_id` int NOT NULL,
  `start_time` datetime NOT NULL,
  `end_time` datetime NOT NULL,
  `bill_mins` int DEFAULT '0',
  `billable` varchar(1) NOT NULL,
  `bill_code` varchar(15) DEFAULT NULL,
  `provider_id` varchar(30) NOT NULL DEFAULT '1',
  `action_id` int DEFAULT NULL,
  PRIMARY KEY (`bill_id`),
  KEY `idx_bill_entry_provider_bill_code` (`provider_id`,`bill_code`),
  KEY `idx_bill_entry_web_user_id` (`web_user_id`)
) ENGINE=MyISAM AUTO_INCREMENT=90244 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `bill_expected`
--

DROP TABLE IF EXISTS `bill_expected`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bill_expected` (
  `username` varchar(30) NOT NULL,
  `web_user_id` int NOT NULL,
  `bill_date` date NOT NULL,
  `bill_mins` int NOT NULL DEFAULT '0',
  `bill_amount` int NOT NULL DEFAULT '0',
  `work_status` varchar(1) DEFAULT NULL,
  PRIMARY KEY (`web_user_id`,`bill_date`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `bill_month`
--

DROP TABLE IF EXISTS `bill_month`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bill_month` (
  `bill_month_id` int NOT NULL AUTO_INCREMENT,
  `bill_code` varchar(15) NOT NULL,
  `provider_id` varchar(30) NOT NULL DEFAULT '1',
  `bill_date` date NOT NULL,
  `bill_mins_expected` int NOT NULL,
  `bill_mins_actual` int NOT NULL,
  `bill_budget_id` int DEFAULT NULL,
  PRIMARY KEY (`bill_month_id`),
  UNIQUE KEY `uk_bill_month_provider_code_date` (`provider_id`,`bill_code`,`bill_date`),
  KEY `idx_bill_month_provider_bill_code` (`provider_id`,`bill_code`)
) ENGINE=MyISAM AUTO_INCREMENT=278 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `bill_work_status`
--

DROP TABLE IF EXISTS `bill_work_status`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bill_work_status` (
  `work_status` varchar(1) NOT NULL,
  `work_label` varchar(30) NOT NULL,
  PRIMARY KEY (`work_status`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `contact_event`
--

DROP TABLE IF EXISTS `contact_event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `contact_event` (
  `event_id` int NOT NULL AUTO_INCREMENT,
  `contact_id` int NOT NULL,
  `event_type` varchar(1) NOT NULL,
  `event_num` int NOT NULL,
  `event_date` date NOT NULL,
  PRIMARY KEY (`event_id`)
) ENGINE=MyISAM AUTO_INCREMENT=4 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project`
--

DROP TABLE IF EXISTS `project`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project` (
  `project_id` int NOT NULL AUTO_INCREMENT,
  `category_code` varchar(15) DEFAULT NULL,
  `project_name` varchar(100) NOT NULL,
  `provider_name` varchar(45) DEFAULT NULL,
  `description` varchar(1200) DEFAULT NULL,
  `phase_code` varchar(4) DEFAULT 'Unkn',
  `profile_id` int DEFAULT NULL,
  `bill_code` varchar(15) DEFAULT '.',
  `provider_id` varchar(30) DEFAULT NULL,
  `username` varchar(30) NOT NULL DEFAULT 'nbunker',
  `web_user_id` int NOT NULL,
  `priority_level` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`project_id`),
  KEY `idx_project_provider_bill_code` (`provider_id`,`bill_code`),
  KEY `idx_project_web_user_id` (`web_user_id`)
) ENGINE=MyISAM AUTO_INCREMENT=48708 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_action`
--

DROP TABLE IF EXISTS `project_action`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_action` (
  `action_id` int NOT NULL AUTO_INCREMENT,
  `project_id` int NOT NULL,
  `contact_id` int NOT NULL,
  `action_date` datetime NOT NULL,
  `action_description` varchar(12000) DEFAULT NULL,
  `next_description` varchar(1200) DEFAULT NULL,
  `next_due` datetime DEFAULT NULL,
  `next_action_type` varchar(16) DEFAULT NULL,
  `next_time_estimate` int DEFAULT '0',
  `next_contact_id` int DEFAULT NULL,
  `next_action_id` int DEFAULT '0',
  `provider_id` varchar(30) NOT NULL DEFAULT '1',
  `priority_level` int NOT NULL DEFAULT '1',
  `next_deadline` datetime DEFAULT NULL,
  `goal_status` varchar(1) DEFAULT NULL,
  `template_action_id` int DEFAULT '0',
  `link_url` varchar(1200) DEFAULT NULL,
  `next_time_actual` int DEFAULT NULL,
  `next_notes` text,
  `next_summary` text,
  `template_type` varchar(1) DEFAULT NULL,
  `next_feedback` text,
  `priority_special` varchar(1) DEFAULT NULL,
  `next_action_status` varchar(1) DEFAULT NULL,
  `next_change_date` datetime DEFAULT NULL,
  PRIMARY KEY (`action_id`)
) ENGINE=MyISAM AUTO_INCREMENT=786630 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_action_change_log`
--

DROP TABLE IF EXISTS `project_action_change_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_action_change_log` (
  `change_id` int NOT NULL AUTO_INCREMENT,
  `action_id` int NOT NULL,
  `project_id` int NOT NULL,
  `proposal_id` int DEFAULT NULL,
  `change_date` datetime NOT NULL,
  `actor_type` varchar(16) NOT NULL,
  `actor_id` varchar(60) DEFAULT NULL,
  `source_type` varchar(24) DEFAULT NULL,
  `change_summary` text,
  `change_patch` text,
  `change_reason` text,
  PRIMARY KEY (`change_id`),
  KEY `idx_change_action_date` (`action_id`,`change_date`),
  KEY `idx_change_project_date` (`project_id`,`change_date`),
  KEY `idx_change_proposal_date` (`proposal_id`,`change_date`),
  KEY `idx_change_actor_date` (`actor_type`,`change_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_action_next`
--

DROP TABLE IF EXISTS `project_action_next`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_action_next` (
  `action_next_id` int NOT NULL AUTO_INCREMENT,
  `project_id` int NOT NULL,
  `contact_id` int NOT NULL,
  `provider_id` varchar(30) NOT NULL DEFAULT '1',
  `next_action_status` varchar(1) NOT NULL,
  `next_change_date` datetime DEFAULT NULL,
  `next_description` varchar(1200) NOT NULL,
  `next_due` datetime DEFAULT NULL,
  `next_action_type` varchar(16) DEFAULT NULL,
  `next_time_estimate` int DEFAULT '0',
  `next_contact_id` int DEFAULT NULL,
  `priority_level` int NOT NULL DEFAULT '1',
  `completion_order` int NOT NULL DEFAULT '0',
  `next_deadline` datetime DEFAULT NULL,
  `goal_status` varchar(1) DEFAULT NULL,
  `template_action_next_id` int DEFAULT NULL,
  `link_url` varchar(1200) DEFAULT NULL,
  `next_time_actual` int DEFAULT NULL,
  `next_notes` text,
  `next_summary` text,
  `template_type` varchar(1) DEFAULT NULL,
  `next_feedback` text,
  `priority_special` varchar(1) DEFAULT NULL,
  PRIMARY KEY (`action_next_id`),
  KEY `idx_pan_project_due` (`project_id`,`next_due`),
  KEY `idx_pan_project_deadline` (`project_id`,`next_deadline`),
  KEY `idx_pan_contact` (`contact_id`),
  KEY `idx_pan_next_contact` (`next_contact_id`),
  KEY `idx_pan_status` (`next_action_status`)
) ENGINE=InnoDB AUTO_INCREMENT=16384 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_action_proposal`
--

DROP TABLE IF EXISTS `project_action_proposal`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_action_proposal` (
  `proposal_id` int NOT NULL AUTO_INCREMENT,
  `client_id` int NOT NULL,
  `project_id` int NOT NULL,
  `action_id` int DEFAULT NULL,
  `contact_id` int DEFAULT NULL,
  `proposal_status` varchar(16) NOT NULL DEFAULT 'new',
  `proposal_create_date` datetime NOT NULL,
  `proposal_decide_date` datetime DEFAULT NULL,
  `model_name` varchar(80) DEFAULT NULL,
  `request_id` varchar(120) DEFAULT NULL,
  `proposed_summary` text,
  `proposed_rationale` text,
  `proposed_patch` text,
  `input_snapshot` text,
  PRIMARY KEY (`proposal_id`),
  KEY `idx_proposal_client_project` (`client_id`,`project_id`),
  KEY `idx_proposal_client_action` (`client_id`,`action_id`),
  KEY `idx_proposal_project_date` (`project_id`,`proposal_create_date`),
  KEY `idx_proposal_project_status` (`project_id`,`proposal_status`),
  KEY `idx_proposal_action_status` (`action_id`,`proposal_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_action_taken`
--

DROP TABLE IF EXISTS `project_action_taken`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_action_taken` (
  `action_taken_id` int NOT NULL AUTO_INCREMENT,
  `project_id` int NOT NULL,
  `contact_id` int NOT NULL,
  `action_date` datetime NOT NULL,
  `action_description` varchar(12000) NOT NULL,
  `provider_id` varchar(30) NOT NULL DEFAULT '1',
  PRIMARY KEY (`action_taken_id`),
  KEY `idx_pat_project_date` (`project_id`,`action_date`),
  KEY `idx_pat_contact_date` (`contact_id`,`action_date`)
) ENGINE=InnoDB AUTO_INCREMENT=16384 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_address`
--

DROP TABLE IF EXISTS `project_address`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_address` (
  `address_id` int NOT NULL AUTO_INCREMENT,
  `address_status` varchar(1) DEFAULT NULL,
  `country` varchar(250) DEFAULT NULL,
  `line1` varchar(250) DEFAULT NULL,
  `line2` varchar(250) DEFAULT NULL,
  `line3` varchar(250) DEFAULT NULL,
  `line4` varchar(250) DEFAULT NULL,
  `city` varchar(250) DEFAULT NULL,
  `state` varchar(250) DEFAULT NULL,
  `zip` varchar(250) DEFAULT NULL,
  `time_zone` varchar(250) DEFAULT NULL,
  PRIMARY KEY (`address_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_area`
--

DROP TABLE IF EXISTS `project_area`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_area` (
  `area_id` int NOT NULL,
  `area_label` varchar(60) NOT NULL,
  `username` varchar(30) NOT NULL,
  `web_user_id` int NOT NULL,
  `sort_order` int DEFAULT NULL,
  `visible` varchar(1) DEFAULT NULL,
  PRIMARY KEY (`area_id`),
  KEY `idx_project_area_web_user_id` (`web_user_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_area_assigned`
--

DROP TABLE IF EXISTS `project_area_assigned`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_area_assigned` (
  `area_id` int NOT NULL,
  `project_id` int NOT NULL,
  PRIMARY KEY (`area_id`,`project_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_bookmark`
--

DROP TABLE IF EXISTS `project_bookmark`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_bookmark` (
  `project_id` int NOT NULL,
  `bookmark_label` varchar(250) NOT NULL,
  `bookmark_url` varchar(500) NOT NULL,
  PRIMARY KEY (`project_id`,`bookmark_label`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_category`
--

DROP TABLE IF EXISTS `project_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_category` (
  `category_code` varchar(15) NOT NULL DEFAULT '',
  `client_name` varchar(150) DEFAULT NULL,
  `sort_order` int DEFAULT NULL,
  `visible` varchar(1) DEFAULT NULL,
  `client_acronym` varchar(15) DEFAULT NULL,
  `provider_id` varchar(30) NOT NULL DEFAULT '1',
  `project_category_id` int NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`project_category_id`)
) ENGINE=MyISAM AUTO_INCREMENT=117 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_contact`
--

DROP TABLE IF EXISTS `project_contact`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_contact` (
  `contact_id` int NOT NULL AUTO_INCREMENT,
  `name_last` varchar(60) NOT NULL,
  `name_first` varchar(60) NOT NULL,
  `name_title` varchar(10) DEFAULT NULL,
  `organization_name` varchar(90) DEFAULT NULL,
  `department_name` varchar(90) DEFAULT NULL,
  `position_title` varchar(90) DEFAULT NULL,
  `phone_number` varchar(30) DEFAULT NULL,
  `email_address` varchar(60) DEFAULT NULL,
  `contact_info` varchar(1500) DEFAULT NULL,
  `time_zone` varchar(60) DEFAULT NULL,
  `email_alert` varchar(1) NOT NULL DEFAULT 'N',
  `provider_id` varchar(30) NOT NULL DEFAULT '1',
  `phone_textable` varchar(1) NOT NULL DEFAULT 'N',
  `email_confirmed` varchar(1) NOT NULL DEFAULT 'N',
  `address_id` int DEFAULT NULL,
  PRIMARY KEY (`contact_id`)
) ENGINE=MyISAM AUTO_INCREMENT=67099 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_contact_assigned`
--

DROP TABLE IF EXISTS `project_contact_assigned`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_contact_assigned` (
  `contact_id` int NOT NULL,
  `project_id` int NOT NULL,
  `priority` int DEFAULT '0',
  `email_alert` varchar(1) NOT NULL DEFAULT 'N',
  `update_due` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`contact_id`,`project_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_contact_position`
--

DROP TABLE IF EXISTS `project_contact_position`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_contact_position` (
  `contact_id` int NOT NULL,
  `position_date` datetime NOT NULL,
  `position_label` varchar(60) DEFAULT NULL,
  `position_detail` varchar(250) DEFAULT NULL,
  `position_lat` float DEFAULT NULL,
  `position_log` float DEFAULT NULL,
  PRIMARY KEY (`contact_id`,`position_date`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_contact_supervisor`
--

DROP TABLE IF EXISTS `project_contact_supervisor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_contact_supervisor` (
  `contact_supervisor_id` int NOT NULL AUTO_INCREMENT,
  `contact_id` int NOT NULL,
  `supervisor_id` int NOT NULL,
  `email_alert` varchar(1) DEFAULT 'N',
  PRIMARY KEY (`contact_supervisor_id`)
) ENGINE=MyISAM AUTO_INCREMENT=21 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_next_action_type`
--

DROP TABLE IF EXISTS `project_next_action_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_next_action_type` (
  `next_action_type` varchar(16) NOT NULL,
  `next_action_label` varchar(100) NOT NULL,
  PRIMARY KEY (`next_action_type`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_phase`
--

DROP TABLE IF EXISTS `project_phase`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_phase` (
  `phase_code` varchar(4) NOT NULL,
  `phase_label` varchar(30) DEFAULT NULL,
  PRIMARY KEY (`phase_code`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_provider`
--

DROP TABLE IF EXISTS `project_provider`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_provider` (
  `provider_id` varchar(30) NOT NULL,
  `provider_name` varchar(255) NOT NULL,
  PRIMARY KEY (`provider_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `report_profile`
--

DROP TABLE IF EXISTS `report_profile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report_profile` (
  `profile_id` int NOT NULL AUTO_INCREMENT,
  `extends_profile_id` int DEFAULT NULL,
  `profile_label` varchar(50) DEFAULT NULL,
  `provider_id` varchar(30) DEFAULT NULL,
  `username` varchar(30) DEFAULT NULL,
  `web_user_id` int DEFAULT NULL,
  `profile_type` varchar(3) DEFAULT NULL,
  `use_status` varchar(1) DEFAULT NULL,
  `extend_status` varchar(1) DEFAULT NULL,
  `context_type` varchar(12) DEFAULT NULL,
  `selector_type` varchar(12) DEFAULT NULL,
  `definition` blob,
  PRIMARY KEY (`profile_id`),
  KEY `idx_report_profile_web_user_id` (`web_user_id`)
) ENGINE=MyISAM AUTO_INCREMENT=1036 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `report_schedule`
--

DROP TABLE IF EXISTS `report_schedule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report_schedule` (
  `profile_id` int NOT NULL,
  `date_start` datetime NOT NULL,
  `method` varchar(1) NOT NULL,
  `period` varchar(2) NOT NULL,
  `location` varchar(120) DEFAULT NULL,
  `status` varchar(1) DEFAULT 'S',
  `name` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`profile_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tracker_keys`
--

DROP TABLE IF EXISTS `tracker_keys`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tracker_keys` (
  `key_name` varchar(100) NOT NULL,
  `key_type` int NOT NULL,
  `key_id` varchar(30) NOT NULL,
  `key_value` varchar(400) DEFAULT NULL,
  `key_content` blob,
  PRIMARY KEY (`key_name`,`key_type`,`key_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `web_api_client`
--

DROP TABLE IF EXISTS `web_api_client`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `web_api_client` (
  `client_id` int NOT NULL AUTO_INCREMENT,
  `api_key` varchar(80) NOT NULL,
  `username` varchar(30) NOT NULL,
  `web_user_id` int NOT NULL,
  `provider_id` varchar(30) DEFAULT NULL,
  `agent_name` varchar(80) DEFAULT NULL,
  `enabled` char(1) NOT NULL DEFAULT 'Y',
  `create_date` datetime NOT NULL,
  `last_used_date` datetime DEFAULT NULL,
  PRIMARY KEY (`client_id`),
  UNIQUE KEY `uk_web_api_client_key` (`api_key`),
  KEY `idx_web_api_client_user` (`username`),
  KEY `idx_web_api_client_web_user_id` (`web_user_id`),
  KEY `idx_web_api_client_provider` (`provider_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `web_user`
--

DROP TABLE IF EXISTS `web_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `web_user` (
  `web_user_id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(30) NOT NULL,
  `contact_id` int NOT NULL,
  `password` varchar(30) DEFAULT NULL,
  `provider_id` varchar(30) NOT NULL DEFAULT '1',
  `user_type` varchar(30) NOT NULL DEFAULT 'user',
  PRIMARY KEY (`web_user_id`),
  UNIQUE KEY `uk_web_user_username` (`username`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-02-09 10:14:50
