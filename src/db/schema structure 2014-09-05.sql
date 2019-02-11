-- phpMyAdmin SQL Dump
-- version 3.5.4
-- http://www.phpmyadmin.net
--
-- Host: 127.0.0.1
-- Generation Time: Sep 05, 2014 at 08:47 AM
-- Server version: 5.0.95-log
-- PHP Version: 5.3.28

SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- Database: `openimmu_tracker`
--

-- --------------------------------------------------------

--
-- Table structure for table `bill_budget`
--

CREATE TABLE IF NOT EXISTS `bill_budget` (
  `bill_budget_id` int(11) NOT NULL auto_increment,
  `bill_budget_code` varchar(30) NOT NULL,
  `bill_code` varchar(15) NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `bill_mins` int(11) NOT NULL,
  `bill_mins_remaining` int(11) default '0',
  PRIMARY KEY  (`bill_budget_id`),
  UNIQUE KEY `bill_budget_code` (`bill_budget_code`,`bill_code`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 AUTO_INCREMENT=24 ;

-- --------------------------------------------------------

--
-- Table structure for table `bill_code`
--

CREATE TABLE IF NOT EXISTS `bill_code` (
  `bill_code` varchar(15) NOT NULL,
  `bill_label` varchar(30) NOT NULL,
  `billable` varchar(1) NOT NULL,
  `visible` varchar(1) NOT NULL,
  `client_bill_code` varchar(30) default NULL,
  `client_bill_description` varchar(120) default NULL,
  `provider_id` varchar(30) NOT NULL default '1',
  `estimate_min` int(11) NOT NULL default '0',
  `bill_rate` int(11) NOT NULL default '0',
  `bill_round` int(11) NOT NULL default '1',
  PRIMARY KEY  (`bill_code`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `bill_day`
--

CREATE TABLE IF NOT EXISTS `bill_day` (
  `bill_day_id` int(11) NOT NULL auto_increment,
  `bill_code` varchar(15) NOT NULL,
  `bill_date` date NOT NULL,
  `bill_mins` int(11) NOT NULL,
  `bill_budget_id` int(11) default NULL,
  `bill_month_id` int(11) default NULL,
  `bill_mins_budget` int(11) default '0',
  PRIMARY KEY  (`bill_day_id`),
  UNIQUE KEY `bill_code` (`bill_code`,`bill_date`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 AUTO_INCREMENT=2439 ;

-- --------------------------------------------------------

--
-- Table structure for table `bill_entry`
--

CREATE TABLE IF NOT EXISTS `bill_entry` (
  `bill_id` int(11) NOT NULL auto_increment,
  `project_id` int(11) NOT NULL,
  `client_code` varchar(5) NOT NULL,
  `username` varchar(30) NOT NULL,
  `start_time` datetime NOT NULL,
  `end_time` datetime NOT NULL,
  `bill_mins` int(11) default '0',
  `billable` varchar(1) NOT NULL,
  `bill_code` varchar(15) default NULL,
  `provider_id` varchar(30) NOT NULL default '1',
  PRIMARY KEY  (`bill_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 AUTO_INCREMENT=42069 ;

-- --------------------------------------------------------

--
-- Table structure for table `bill_expected`
--

CREATE TABLE IF NOT EXISTS `bill_expected` (
  `username` varchar(30) NOT NULL,
  `bill_date` date NOT NULL,
  `bill_mins` int(11) NOT NULL default '0',
  `bill_amount` int(11) NOT NULL default '0',
  `work_status` varchar(1) default NULL,
  PRIMARY KEY  (`username`,`bill_date`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `bill_month`
--

CREATE TABLE IF NOT EXISTS `bill_month` (
  `bill_month_id` int(11) NOT NULL auto_increment,
  `bill_code` varchar(15) NOT NULL,
  `bill_date` date NOT NULL,
  `bill_mins_expected` int(11) NOT NULL,
  `bill_mins_actual` int(11) NOT NULL,
  `bill_budget_id` int(11) default NULL,
  PRIMARY KEY  (`bill_month_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 AUTO_INCREMENT=227 ;

-- --------------------------------------------------------

--
-- Table structure for table `bill_work_status`
--

CREATE TABLE IF NOT EXISTS `bill_work_status` (
  `work_status` varchar(1) NOT NULL,
  `work_label` varchar(30) NOT NULL,
  PRIMARY KEY  (`work_status`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `budget_account`
--

CREATE TABLE IF NOT EXISTS `budget_account` (
  `account_id` int(11) NOT NULL auto_increment,
  `account_label` varchar(30) NOT NULL,
  `provider_id` varchar(30) NOT NULL,
  `start_amount` int(11) NOT NULL,
  `start_date` date NOT NULL,
  `balance_amount` int(11) NOT NULL,
  `balance_date` date NOT NULL,
  PRIMARY KEY  (`account_id`),
  UNIQUE KEY `account_label` (`account_label`,`provider_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 AUTO_INCREMENT=7 ;

-- --------------------------------------------------------

--
-- Table structure for table `budget_item`
--

CREATE TABLE IF NOT EXISTS `budget_item` (
  `item_id` int(11) NOT NULL auto_increment,
  `item_label` varchar(30) NOT NULL,
  `account_id` int(11) NOT NULL,
  `item_status` varchar(1) NOT NULL,
  `last_amount` int(11) NOT NULL,
  `last_date` date NOT NULL,
  `priority_code` varchar(1) NOT NULL,
  `related_item_id` int(11) default NULL,
  PRIMARY KEY  (`item_id`),
  UNIQUE KEY `item_label` (`item_label`,`account_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 AUTO_INCREMENT=92 ;

-- --------------------------------------------------------

--
-- Table structure for table `budget_month`
--

CREATE TABLE IF NOT EXISTS `budget_month` (
  `month_id` int(11) NOT NULL auto_increment,
  `month_date` date NOT NULL,
  `account_id` int(11) NOT NULL,
  `balance_start` int(11) NOT NULL,
  `balance_end` int(11) NOT NULL,
  PRIMARY KEY  (`month_id`),
  UNIQUE KEY `month_date` (`month_date`,`account_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 AUTO_INCREMENT=24 ;

-- --------------------------------------------------------

--
-- Table structure for table `budget_trans`
--

CREATE TABLE IF NOT EXISTS `budget_trans` (
  `trans_id` int(11) NOT NULL auto_increment,
  `item_id` int(11) NOT NULL,
  `month_id` int(11) NOT NULL,
  `trans_date` date NOT NULL,
  `trans_status` varchar(1) NOT NULL,
  `trans_amount` int(11) NOT NULL,
  `related_trans_id` int(11) default NULL,
  `trans_record_id` int(11) default NULL,
  PRIMARY KEY  (`trans_id`),
  UNIQUE KEY `item_id` (`item_id`,`month_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 AUTO_INCREMENT=237 ;

-- --------------------------------------------------------

--
-- Table structure for table `budget_trans_record`
--

CREATE TABLE IF NOT EXISTS `budget_trans_record` (
  `trans_record_id` int(11) NOT NULL auto_increment,
  `account_id` int(11) NOT NULL,
  `trans_id` int(11) default NULL,
  `trans_date` date NOT NULL,
  `trans_amount` int(11) NOT NULL,
  `description` varchar(300) NOT NULL,
  PRIMARY KEY  (`trans_record_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 AUTO_INCREMENT=117 ;

-- --------------------------------------------------------

--
-- Table structure for table `contact_event`
--

CREATE TABLE IF NOT EXISTS `contact_event` (
  `event_id` int(11) NOT NULL auto_increment,
  `contact_id` int(11) NOT NULL,
  `event_type` varchar(1) NOT NULL,
  `event_num` int(11) NOT NULL,
  `event_date` date NOT NULL,
  PRIMARY KEY  (`event_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 AUTO_INCREMENT=4 ;

-- --------------------------------------------------------

--
-- Table structure for table `project`
--

CREATE TABLE IF NOT EXISTS `project` (
  `project_id` int(11) NOT NULL auto_increment,
  `client_code` varchar(5) default NULL,
  `project_name` varchar(100) NOT NULL,
  `provider_name` varchar(45) default NULL,
  `vendor_name` varchar(45) default NULL,
  `system_name` varchar(30) default NULL,
  `description` varchar(1200) default NULL,
  `phase_code` varchar(4) default 'Unkn',
  `profile_id` int(11) default NULL,
  `bill_code` varchar(15) default '.',
  `file_location` varchar(60) default NULL,
  `provider_id` varchar(30) default NULL,
  `username` varchar(30) NOT NULL default 'nbunker',
  `iis_submission_code` varchar(30) default NULL,
  `iis_facility_id` varchar(30) default NULL,
  `medical_organization` varchar(60) default NULL,
  `iis_region_code` varchar(30) default NULL,
  `priority_level` int(11) NOT NULL default '0',
  PRIMARY KEY  (`project_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 AUTO_INCREMENT=47266 ;

-- --------------------------------------------------------

--
-- Table structure for table `project_action`
--

CREATE TABLE IF NOT EXISTS `project_action` (
  `action_id` int(11) NOT NULL auto_increment,
  `project_id` int(11) NOT NULL,
  `contact_id` int(11) NOT NULL,
  `action_date` datetime NOT NULL,
  `action_description` varchar(12000) default NULL,
  `next_description` varchar(1200) default NULL,
  `next_due` datetime default NULL,
  `next_action_type` varchar(1) default NULL,
  `next_time_estimate` int(11) default '0',
  `next_contact_id` int(11) default NULL,
  `next_action_id` int(11) default '0',
  `provider_id` varchar(30) NOT NULL default '1',
  `priority_level` int(11) NOT NULL default '1',
  PRIMARY KEY  (`action_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 AUTO_INCREMENT=741336 ;

-- --------------------------------------------------------

--
-- Table structure for table `project_area`
--

CREATE TABLE IF NOT EXISTS `project_area` (
  `area_id` int(11) NOT NULL,
  `area_label` varchar(60) NOT NULL,
  `username` varchar(30) NOT NULL,
  `sort_order` int(11) default NULL,
  `visible` varchar(1) default NULL,
  PRIMARY KEY  (`area_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `project_area_assigned`
--

CREATE TABLE IF NOT EXISTS `project_area_assigned` (
  `area_id` int(11) NOT NULL,
  `project_id` int(11) NOT NULL,
  PRIMARY KEY  (`area_id`,`project_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `project_bookmark`
--

CREATE TABLE IF NOT EXISTS `project_bookmark` (
  `project_id` int(11) NOT NULL,
  `bookmark_label` varchar(250) NOT NULL,
  `bookmark_url` varchar(500) NOT NULL,
  PRIMARY KEY  (`project_id`,`bookmark_label`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `project_client`
--

CREATE TABLE IF NOT EXISTS `project_client` (
  `client_code` varchar(5) NOT NULL,
  `client_name` varchar(30) NOT NULL,
  `sort_order` int(11) default NULL,
  `visible` varchar(1) default NULL,
  `client_acronym` varchar(15) default NULL,
  `provider_id` varchar(30) NOT NULL default '1',
  PRIMARY KEY  (`client_code`,`provider_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `project_contact`
--

CREATE TABLE IF NOT EXISTS `project_contact` (
  `contact_id` int(11) NOT NULL auto_increment,
  `name_last` varchar(60) NOT NULL,
  `name_first` varchar(60) NOT NULL,
  `name_title` varchar(10) default NULL,
  `organization_name` varchar(90) default NULL,
  `department_name` varchar(90) default NULL,
  `position_title` varchar(90) default NULL,
  `number_phone` varchar(30) default NULL,
  `number_cell` varchar(30) default NULL,
  `number_pager` varchar(30) default NULL,
  `number_fax` varchar(30) default NULL,
  `email` varchar(60) default NULL,
  `address_street1` varchar(60) default NULL,
  `address_street2` varchar(60) default NULL,
  `address_city` varchar(60) default NULL,
  `address_state` varchar(60) default NULL,
  `address_zip` varchar(15) default NULL,
  `address_country` varchar(60) default NULL,
  `address_lat` float default NULL,
  `address_long` float default NULL,
  `contact_info` varchar(1500) default NULL,
  `time_zone` varchar(60) default NULL,
  `email_alert` varchar(1) NOT NULL default 'N',
  `provider_id` varchar(30) NOT NULL default '1',
  PRIMARY KEY  (`contact_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 AUTO_INCREMENT=66627 ;

-- --------------------------------------------------------

--
-- Table structure for table `project_contact_assigned`
--

CREATE TABLE IF NOT EXISTS `project_contact_assigned` (
  `contact_id` int(11) NOT NULL,
  `project_id` int(11) NOT NULL,
  `priority` int(11) default '0',
  `email_alert` varchar(1) NOT NULL default 'N',
  `update_due` int(11) NOT NULL default '0',
  PRIMARY KEY  (`contact_id`,`project_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `project_contact_position`
--

CREATE TABLE IF NOT EXISTS `project_contact_position` (
  `contact_id` int(11) NOT NULL,
  `position_date` datetime NOT NULL,
  `position_label` varchar(60) default NULL,
  `position_detail` varchar(250) default NULL,
  `position_lat` float default NULL,
  `position_log` float default NULL,
  PRIMARY KEY  (`contact_id`,`position_date`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `project_next_action_type`
--

CREATE TABLE IF NOT EXISTS `project_next_action_type` (
  `next_action_type` varchar(1) NOT NULL,
  `next_action_label` varchar(100) NOT NULL,
  PRIMARY KEY  (`next_action_type`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `project_phase`
--

CREATE TABLE IF NOT EXISTS `project_phase` (
  `phase_code` varchar(4) NOT NULL,
  `phase_label` varchar(30) default NULL,
  PRIMARY KEY  (`phase_code`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `project_provider`
--

CREATE TABLE IF NOT EXISTS `project_provider` (
  `provider_id` varchar(30) NOT NULL,
  `provider_name` varchar(255) NOT NULL,
  PRIMARY KEY  (`provider_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `report_profile`
--

CREATE TABLE IF NOT EXISTS `report_profile` (
  `profile_id` int(11) NOT NULL auto_increment,
  `extends_profile_id` int(11) default NULL,
  `profile_label` varchar(50) default NULL,
  `provider_id` varchar(30) default NULL,
  `username` varchar(30) default NULL,
  `profile_type` varchar(3) default NULL,
  `use_status` varchar(1) default NULL,
  `extend_status` varchar(1) default NULL,
  `context_type` varchar(12) default NULL,
  `selector_type` varchar(12) default NULL,
  `definition` blob,
  PRIMARY KEY  (`profile_id`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 AUTO_INCREMENT=1033 ;

-- --------------------------------------------------------

--
-- Table structure for table `report_schedule`
--

CREATE TABLE IF NOT EXISTS `report_schedule` (
  `profile_id` int(11) NOT NULL,
  `date_start` datetime NOT NULL,
  `method` varchar(1) NOT NULL,
  `period` varchar(2) NOT NULL,
  `location` varchar(120) default NULL,
  `status` varchar(1) default 'S',
  `name` varchar(45) default NULL,
  PRIMARY KEY  (`profile_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `sequence`
--

CREATE TABLE IF NOT EXISTS `sequence` (
  `table_name` varchar(60) NOT NULL,
  `sequence_id` int(11) NOT NULL,
  PRIMARY KEY  (`table_name`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `tracker_keys`
--

CREATE TABLE IF NOT EXISTS `tracker_keys` (
  `key_name` varchar(100) NOT NULL,
  `key_type` int(11) NOT NULL,
  `key_id` varchar(30) NOT NULL,
  `key_value` varchar(400) default NULL,
  `key_content` blob,
  PRIMARY KEY  (`key_name`,`key_type`,`key_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `web_user`
--

CREATE TABLE IF NOT EXISTS `web_user` (
  `username` varchar(30) NOT NULL,
  `contact_id` int(11) NOT NULL,
  `password` varchar(30) default NULL,
  `provider_id` varchar(30) NOT NULL default '1',
  `user_type` varchar(30) NOT NULL default 'user',
  `parent_username` varchar(30) default NULL,
  PRIMARY KEY  (`username`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
