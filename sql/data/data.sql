-- MySQL dump 10.13  Distrib 5.6.19, for osx10.9 (x86_64)
--
-- Host: 127.0.0.1    Database: keystone
-- ------------------------------------------------------
-- Server version	5.5.5-10.0.20-MariaDB-1~trusty-wsrep
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Dumping data for table `access_token`
--


--
-- Dumping data for table `alembic_version`
--

INSERT INTO `alembic_version` VALUES ('39c917e1358c');

--
-- Dumping data for table `assignment`
--

INSERT INTO `assignment` VALUES ('UserProject','10071819','-543569094','6',0);
INSERT INTO `assignment` VALUES ('UserProject','10071819','StagingUS_e99825fd-b069-4987-bf46-42766bf79fd3','5',0);
INSERT INTO `assignment` VALUES ('UserProject','173189','983452','5',0);
INSERT INTO `assignment` VALUES ('UserProject','173189','identity','1',0);
INSERT INTO `assignment` VALUES ('UserProject','3a4407a6b3e349998478409c5b7eda14','983452','6',0);
INSERT INTO `assignment` VALUES ('UserProject','3a4407a6b3e349998478409c5b7eda14','StagingUS_ed361067-aebd-45b7-92a3-353edacb0e09','5',0);
INSERT INTO `assignment` VALUES ('UserProject','4e997592aad24e2183e51bd013f223c5','78543988','6',0);
INSERT INTO `assignment` VALUES ('UserProject','4e997592aad24e2183e51bd013f223c5','identity','3',0);
INSERT INTO `assignment` VALUES ('UserProject','4e997592aad24e2183e51bd013f223c5','MossoCloudFS_78543988','5',0);
INSERT INTO `assignment` VALUES ('UserProject','d8f1b8df14c844d9919aee4e19a2f6f9','78543995','6',0);
INSERT INTO `assignment` VALUES ('UserProject','d8f1b8df14c844d9919aee4e19a2f6f9','identity','3',0);
INSERT INTO `assignment` VALUES ('UserProject','d8f1b8df14c844d9919aee4e19a2f6f9','MossoCloudFS_78543995','5',0);

--
-- Dumping data for table `bypass_code_rax`
--

INSERT INTO `bypass_code_rax` VALUES ('662a2c23d2cb4e74917438c33c573203','2iVMIQGaOuOaHWLvkV578NeT0pItdTUwcdNqAxr1qw6p2DtXn3QDCTlbGyVi8HiD1gDUGxY3BeLn/u/o0KDMsw==');
INSERT INTO `bypass_code_rax` VALUES ('662a2c23d2cb4e74917438c33c573203','ClBkhm08CWqqG0m7rqweRi1x2AI700AOENw5pKhf03G4XuBwOwkQ40dEnNNAJ+UCYdFQEy1HcLkWgynLwL3GWQ==');

--
-- Dumping data for table `bypass_device_rax`
--

INSERT INTO `bypass_device_rax` VALUES ('662a2c23d2cb4e74917438c33c573203','057f1b3e8811403d973814e5ed1b41bb','2025-05-20','g/ClOTCIX0J/Og3CReA49A==',10000);

--
-- Dumping data for table `capability_rax`
--


--
-- Dumping data for table `capability_resource_rax`
--


--
-- Dumping data for table `consumer`
--


--
-- Dumping data for table `credential`
--


--
-- Dumping data for table `domain`
--

INSERT INTO `domain` VALUES ('78543988','78543988',1,'{\"description\": \"Default Domain\"}');
INSERT INTO `domain` VALUES ('78543995','78543995',1,'{\"description\": \"Default Domain\"}');
INSERT INTO `domain` VALUES ('983452','983452',1,'{\"description\": \"Default Domain\"}');
INSERT INTO `domain` VALUES ('default','Default',1,'{\"description\": \"Owns users and tenants (i.e. projects) available on Identity API v2.\"}');

--
-- Dumping data for table `domain_rax`
--


--
-- Dumping data for table `endpoint`
--

INSERT INTO `endpoint` VALUES ('1b272e4d0d2d4a298f2c3ffb740beb01','1008','public','6047d506862b81d6d99273b4853adfe81e0478c1','https://storage.stg.swift.racklabs.com/v1','{}',1,'LON');
INSERT INTO `endpoint` VALUES ('2de8d9b78ce743f9a9d82a87ee19db51','1','public','6047d506862b81d6d99273b4853adfe81e0478c1','https://storage101.dfw1.stg.swift.racklabs.com/v1/v1','{}',1,'DFW');
INSERT INTO `endpoint` VALUES ('3807aff2441e4f4d8215659383adcaa4','1015','public','a45b14e394a57e3fd4e45d59ff3693ead204998b','https://servers.staging.ord1.uk.ccp.rackspace.net/v1.0','{}',1,'LON');
INSERT INTO `endpoint` VALUES ('3f242a5d284b419db40f64d4db622cd9','1000','public','6047d506862b81d6d99273b4853adfe81e0478c1','https://otherstorage.clouddrive.com/v1','{}',1,'ORD');
INSERT INTO `endpoint` VALUES ('487a93f2a2db41dfbcf9cf6163cb70cc','1007','internal','6047d506862b81d6d99273b4853adfe81e0478c1','https://snet-storage101.dfw1.stg.swift.racklabs.com','{}',1,'DFW');
INSERT INTO `endpoint` VALUES ('5b1a4b0cf9fe46838420e5c562ce6dde','1003','internal','6047d506862b81d6d99273b4853adfe81e0478c1','https://snet-storage.stg.swift.racklabs.com/v1','{}',1,'ORD');
INSERT INTO `endpoint` VALUES ('5e76cabb3196486f893d412460345b20','1008','internal','6047d506862b81d6d99273b4853adfe81e0478c1','https://snet-storage.stg.swift.racklabs.com/v1','{}',1,'LON');
INSERT INTO `endpoint` VALUES ('607dbca22b4f4b0497b9665d3caa0822','16','public','a45b14e394a57e3fd4e45d59ff3693ead204998b','https://ord.servers.api.rackspacecloud.com/v1.0','{}',1,'LON');
INSERT INTO `endpoint` VALUES ('6c04c4dbdba8478aa6d4d9a31d8e59ab','103','public','6047d506862b81d6d99273b4853adfe81e0478c1','https://storage.stg.swift.racklabs.com/v1','{}',1,'ORD');
INSERT INTO `endpoint` VALUES ('81a37718897c4ef6a3412ad0878ed12e','1','internal','6047d506862b81d6d99273b4853adfe81e0478c1','https://snet-storage101.dfw1.stg.swift.racklabs.com/v1/v1','{}',1,'DFW');
INSERT INTO `endpoint` VALUES ('988b93b3fc704179b5f312262cccc616','8','internal','6047d506862b81d6d99273b4853adfe81e0478c1','https://snet-storage.stg.swift.racklabs.com/v1','{}',1,'LON');
INSERT INTO `endpoint` VALUES ('9aa969b27f88499eb955a7583a2af6ae','1007','public','6047d506862b81d6d99273b4853adfe81e0478c1','https://storage.stg.swift.racklabs.com/v1','{}',1,'DFW');
INSERT INTO `endpoint` VALUES ('a2a20254deb84589bfac2418b1d5e222','3','internal','6047d506862b81d6d99273b4853adfe81e0478c1','https://snet-storage.stg.swift.racklabs.com/v1','{}',1,'ORD');
INSERT INTO `endpoint` VALUES ('b53d7c8ba2cc4c2290286416ecedb1a5','8','public','6047d506862b81d6d99273b4853adfe81e0478c1','https://storage.stg.swift.racklabs.com/v1/','{}',1,'LON');
INSERT INTO `endpoint` VALUES ('bc4141b20b2945bb98dc6069f793751a','1016','public','a45b14e394a57e3fd4e45d59ff3693ead204998b','https://ord.servers.api.rackspacecloud.com/v1.0/','{}',1,'LON');
INSERT INTO `endpoint` VALUES ('c8717d9a59c4495092ae041862298011','1000','internal','6047d506862b81d6d99273b4853adfe81e0478c1','https://otherstorage-snet.clouddrive.com/v1','{}',1,'ORD');
INSERT INTO `endpoint` VALUES ('ccdea3d0c1944f99a5cbc0bd994a752b','3','public','6047d506862b81d6d99273b4853adfe81e0478c1','https://storage.stg.swift.racklabs.com/v1','{}',1,'ORD');
INSERT INTO `endpoint` VALUES ('d1e29a733ad24ab583534435afb97ca5','1003','public','6047d506862b81d6d99273b4853adfe81e0478c1','https://storage.stg.swift.racklabs.com/v1','{}',1,'ORD');
INSERT INTO `endpoint` VALUES ('e0c5f1fe707f49b99ed3e615909ac575','1001','public','6047d506862b81d6d99273b4853adfe81e0478c1','https://storage.stg.swift.racklabs.com/v1','{}',1,'DFW');
INSERT INTO `endpoint` VALUES ('e10a91c31fb0431eaa07765a599c7d65','1001','internal','6047d506862b81d6d99273b4853adfe81e0478c1','https://snet-storage101.dfw1.stg.swift.racklabs.com/v1','{}',1,'DFW');
INSERT INTO `endpoint` VALUES ('e6143403cfc44dda9ab5db1d8eb1b3d2','103','internal','6047d506862b81d6d99273b4853adfe81e0478c1','https://snet-storage.stg.swift.racklabs.com/v1','{}',1,'ORD');

--
-- Dumping data for table `endpoint_group`
--


--
-- Dumping data for table `endpoint_rax`
--

INSERT INTO `endpoint_rax` VALUES ('1b272e4d0d2d4a298f2c3ffb740beb01','NAST','object-store',1,0,'cloudFiles',NULL,NULL,NULL,NULL);
INSERT INTO `endpoint_rax` VALUES ('2b1396bf77e747e697dc119d1869f97f','MOSSO','object-store',0,0,'cloudFiles',NULL,'1','https://api.info.uri.com/v1/','https://api.list.uri.com/v1/');
INSERT INTO `endpoint_rax` VALUES ('2de8d9b78ce743f9a9d82a87ee19db51','NAST','object-store',0,0,'cloudFiles',NULL,'1','https://api.info.uri.com/v1/','https://api.list.uri.com/v1/');
INSERT INTO `endpoint_rax` VALUES ('2f4959a6e5024622ab5bd880faf9274f','MOSSO','compute',0,0,'cloudServers',NULL,'1.0','http://api-n01.qe.ord1.us.cloudcompute.rackspace.net:8080/v1.0','http://api-n01.qe.ord1.us.cloudcompute.rackspace.net:8080/v1.0');
INSERT INTO `endpoint_rax` VALUES ('3807aff2441e4f4d8215659383adcaa4','MOSSO','compute',1,0,'cloudServers',NULL,NULL,NULL,NULL);
INSERT INTO `endpoint_rax` VALUES ('3bbe6af761694b4f85004472b88db587','MOSSO','object-store',0,0,'cloudFiles',NULL,'1','https://api.info.uri.com/v1/','https://api.list.uri.com/v1/');
INSERT INTO `endpoint_rax` VALUES ('3f242a5d284b419db40f64d4db622cd9','MOSSO','cloudServers',0,0,'cloudFiles',NULL,NULL,NULL,NULL);
INSERT INTO `endpoint_rax` VALUES ('41891dcd019e4503be21f667cfaed937','NAST','object-store',0,0,'cloudFiles',NULL,NULL,NULL,NULL);
INSERT INTO `endpoint_rax` VALUES ('487a93f2a2db41dfbcf9cf6163cb70cc','NAST','object-store',0,0,'cloudFiles',NULL,NULL,NULL,NULL);
INSERT INTO `endpoint_rax` VALUES ('4d4a038cd37145e79eeedabf5d23ed75','NAST','object-store',0,0,'cloudFiles',NULL,NULL,NULL,NULL);
INSERT INTO `endpoint_rax` VALUES ('51b4f165a28f4e099a2598e65b7e0888','MOSSO','object-store',0,0,'cloudFiles',NULL,'1','https://api.info.uri.com/v1/','https://api.list.uri.com/v1/');
INSERT INTO `endpoint_rax` VALUES ('55d73fa3120e4bf591e8622f9a247d13','NAST','object-store',0,0,'cloudFiles',NULL,NULL,NULL,NULL);
INSERT INTO `endpoint_rax` VALUES ('5b1a4b0cf9fe46838420e5c562ce6dde','NAST','object-store',0,0,'cloudFiles',NULL,NULL,NULL,NULL);
INSERT INTO `endpoint_rax` VALUES ('5e76cabb3196486f893d412460345b20','NAST','object-store',1,0,'cloudFiles',NULL,NULL,NULL,NULL);
INSERT INTO `endpoint_rax` VALUES ('607dbca22b4f4b0497b9665d3caa0822','MOSSO','compute',0,0,'cloudServers',NULL,NULL,NULL,NULL);
INSERT INTO `endpoint_rax` VALUES ('60d63458d7f04086b842a00b5c51473a','NAST','object-store',0,0,'cloudFiles',NULL,NULL,NULL,NULL);
INSERT INTO `endpoint_rax` VALUES ('62af142c1f84438f9d602e2e21bad6f1','MOSSO','object-store',0,0,'cloudFiles',NULL,'1','https://api.info.uri.com/v1/','https://api.list.uri.com/v1/');
INSERT INTO `endpoint_rax` VALUES ('6c04c4dbdba8478aa6d4d9a31d8e59ab','NAST','object-store',1,0,'cloudFiles',NULL,NULL,NULL,NULL);
INSERT INTO `endpoint_rax` VALUES ('7c9eb836f95446e8a112f2448566fe53','NAST','object-store',0,0,'cloudFiles',NULL,NULL,NULL,NULL);
INSERT INTO `endpoint_rax` VALUES ('7e20bd8a15e44009b9c48ca842665021','NAST','object-store',0,0,'cloudFiles',NULL,'1','https://api.info.uri.com/v1/','https://api.list.uri.com/v1/');
INSERT INTO `endpoint_rax` VALUES ('81a37718897c4ef6a3412ad0878ed12e','NAST','object-store',0,0,'cloudFiles',NULL,'1','https://api.info.uri.com/v1/','https://api.list.uri.com/v1/');
INSERT INTO `endpoint_rax` VALUES ('988b93b3fc704179b5f312262cccc616','NAST','object-store',0,0,'cloudFiles',NULL,'1','https://api.info.uri.com/v1/','https://api.list.uri.com/v1/');
INSERT INTO `endpoint_rax` VALUES ('9aa969b27f88499eb955a7583a2af6ae','NAST','object-store',0,0,'cloudFiles',NULL,NULL,NULL,NULL);
INSERT INTO `endpoint_rax` VALUES ('9e71b281c3a24438b362b6be055f0cd5','NAST','object-store',0,0,'cloudFiles',NULL,NULL,NULL,NULL);
INSERT INTO `endpoint_rax` VALUES ('a2a20254deb84589bfac2418b1d5e222','NAST','object-store',1,0,'cloudFiles',NULL,'1','https://api.info.uri.com/v1/','https://api.list.uri.com/v1/');
INSERT INTO `endpoint_rax` VALUES ('a7d413cc633346c1b1d4ce3134d16e83','MOSSO','object-store',0,0,'cloudFiles',NULL,'1','https://api.info.uri.com/v1/','https://api.list.uri.com/v1/');
INSERT INTO `endpoint_rax` VALUES ('afe8c75ddb9f4e3fbb3c8837ece4a5f6','NAST','object-store',0,0,'cloudFiles',NULL,'1','https://api.info.uri.com/v1/','https://api.list.uri.com/v1/');
INSERT INTO `endpoint_rax` VALUES ('b53d7c8ba2cc4c2290286416ecedb1a5','NAST','object-store',0,0,'cloudFiles',NULL,'1','https://api.info.uri.com/v1/','https://api.list.uri.com/v1/');
INSERT INTO `endpoint_rax` VALUES ('bc4141b20b2945bb98dc6069f793751a','MOSSO','compute',0,0,'cloudServers',NULL,NULL,NULL,NULL);
INSERT INTO `endpoint_rax` VALUES ('c8717d9a59c4495092ae041862298011','MOSSO','cloudServers',0,0,'cloudFiles',NULL,NULL,NULL,NULL);
INSERT INTO `endpoint_rax` VALUES ('ccdea3d0c1944f99a5cbc0bd994a752b','NAST','object-store',1,0,'cloudFiles',NULL,'1','https://api.info.uri.com/v1/','https://api.list.uri.com/v1/');
INSERT INTO `endpoint_rax` VALUES ('cd6213fac7284def86cb8239ef46a590','NAST','object-store',0,0,'cloudFiles',NULL,NULL,NULL,NULL);
INSERT INTO `endpoint_rax` VALUES ('d1e29a733ad24ab583534435afb97ca5','NAST','object-store',0,0,'cloudFiles',NULL,NULL,NULL,NULL);
INSERT INTO `endpoint_rax` VALUES ('e0c5f1fe707f49b99ed3e615909ac575','NAST','object-store',0,0,'cloudFiles',NULL,NULL,NULL,NULL);
INSERT INTO `endpoint_rax` VALUES ('e0fb019043cd47249ab4454f35ca8932','MOSSO','object-store',0,0,'cloudFiles',NULL,'1','https://api.info.uri.com/v1/','https://api.list.uri.com/v1/');
INSERT INTO `endpoint_rax` VALUES ('e10a91c31fb0431eaa07765a599c7d65','NAST','object-store',0,0,'cloudFiles',NULL,NULL,NULL,NULL);
INSERT INTO `endpoint_rax` VALUES ('e6143403cfc44dda9ab5db1d8eb1b3d2','NAST','object-store',1,0,'cloudFiles',NULL,NULL,NULL,NULL);
INSERT INTO `endpoint_rax` VALUES ('fd605ec0e5084c559b36bcde79a5272c','NAST','object-store',0,0,'cloudFiles',NULL,NULL,NULL,NULL);

--
-- Dumping data for table `federated_user_group_membership_rax`
--


--
-- Dumping data for table `federated_user_rax`
--


--
-- Dumping data for table `federation_protocol`
--


--
-- Dumping data for table `group`
--


--
-- Dumping data for table `id_mapping`
--


--
-- Dumping data for table `identity_provider`
--


--
-- Dumping data for table `identity_provider_rax`
--

INSERT INTO `identity_provider_rax` VALUES ('dedicated','http://my.rackspace.com','Identity provider for dedicated','-----BEGIN CERTIFICATE-----\nMIIB9TCCAZ+gAwIBAgIJAMpuL8r3peCbMA0GCSqGSIb3DQEBBQUAMFYxCzAJBgNV\nBAYTAlVTMQ4wDAYDVQQIDAVUZXhhczEUMBIGA1UEBwwLU2FuIEFudG9uaW8xITAf\nBgNVBAoMGEludGVybmV0IFdpZGdpdHMgUHR5IEx0ZDAeFw0xMzA3MDkyMjI3NDla\nFw0xNDA3MDkyMjI3NDlaMFYxCzAJBgNVBAYTAlVTMQ4wDAYDVQQIDAVUZXhhczEU\nMBIGA1UEBwwLU2FuIEFudG9uaW8xITAfBgNVBAoMGEludGVybmV0IFdpZGdpdHMg\nUHR5IEx0ZDBcMA0GCSqGSIb3DQEBAQUAA0sAMEgCQQC0tL3PylPxPj1auMN2CNWI\naCsqPcy4LK9OukjxSfUkewOi6kKov9aS2pe0AsC8uwkhPj7Pl+w0lVYyGSrAh58P\nAgMBAAGjUDBOMB0GA1UdDgQWBBQI8Ezg9M8vnXo7it28nUW6xY974jAfBgNVHSME\nGDAWgBQI8Ezg9M8vnXo7it28nUW6xY974jAMBgNVHRMEBTADAQH/MA0GCSqGSIb3\nDQEBBQUAA0EAOo8UpA+lFmFn/DX4lEgShYpcom11O+KS3mf8NJhl2/34wLw0IJ0t\n1Bp6+UKeELUVXUa8eptaQuJnsj6xMbayxw==\n-----END CERTIFICATE-----\n');
INSERT INTO `identity_provider_rax` VALUES ('identityqe','http://identityqe.rackspace.com','Identity provider for testing','-----BEGIN CERTIFICATE-----\nMIIDkDCCAvmgAwIBAgIJAOJP8rL7wR4oMA0GCSqGSIb3DQEBBQUAMIGNMQswCQYD\nVQQGEwJ1czEOMAwGA1UECBMFdGV4YXMxFDASBgNVBAcTC3NhbiBhbnRvbmlvMRIw\nEAYDVQQKEwlyYWNrc3BhY2UxDjAMBgNVBAsTBWNsb3VkMQswCQYDVQQDEwJxZTEn\nMCUGCSqGSIb3DQEJARYYc2hhbnRoaS52ZWR1bGFAZ21haWwuY29tMB4XDTE0MDcx\nNDIxMzg1MFoXDTI0MDcxMzIxMzg1MFowgY0xCzAJBgNVBAYTAnVzMQ4wDAYDVQQI\nEwV0ZXhhczEUMBIGA1UEBxMLc2FuIGFudG9uaW8xEjAQBgNVBAoTCXJhY2tzcGFj\nZTEOMAwGA1UECxMFY2xvdWQxCzAJBgNVBAMTAnFlMScwJQYJKoZIhvcNAQkBFhhz\naGFudGhpLnZlZHVsYUBnbWFpbC5jb20wgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJ\nAoGBAKzYTBfE7RmfnjGdyGYZ5ptOsNfl8A8aUv3w4KTZMotbbaFXK1lQwYzfJ+zy\nkKzrFdAuAU1ukyRLg74rBwNOI8MEqZOF6tbP3VMrJiMTf3GNFt3ni/hSElUcQdMj\nougaPst1pzk4di9N7zHeJRsyjKIFVNMDyIpCj/N0SyTiW0CnAgMBAAGjgfUwgfIw\nHQYDVR0OBBYEFJisiZXA24UB+3n5I7fZVUGzJmMJMIHCBgNVHSMEgbowgbeAFJis\niZXA24UB+3n5I7fZVUGzJmMJoYGTpIGQMIGNMQswCQYDVQQGEwJ1czEOMAwGA1UE\nCBMFdGV4YXMxFDASBgNVBAcTC3NhbiBhbnRvbmlvMRIwEAYDVQQKEwlyYWNrc3Bh\nY2UxDjAMBgNVBAsTBWNsb3VkMQswCQYDVQQDEwJxZTEnMCUGCSqGSIb3DQEJARYY\nc2hhbnRoaS52ZWR1bGFAZ21haWwuY29tggkA4k/ysvvBHigwDAYDVR0TBAUwAwEB\n/zANBgkqhkiG9w0BAQUFAAOBgQB8ObCZ6NwQAi+hqGhp2q1OTUuNdWz4bsjIfZO6\nFQqKPEwQ38kdRsDY8sd7HR+1J2ymwAUFEVIc965avTLt87HxRtNtCJ38YkbTwIUF\nFFm/w3Cwowa0x/JqRBR5sU0DWHROFNQO+78y8aoFxNOuNTSq/usV72KZ5iYb/vI/\nPz3pcw==\n-----END CERTIFICATE-----\n');
INSERT INTO `identity_provider_rax` VALUES ('test','http://test.rackspace.com','Identity provider for testing','-----BEGIN CERTIFICATE-----\nMIICsDCCAhmgAwIBAgIJAPDLIlBzkeCvMA0GCSqGSIb3DQEBBQUAMEUxCzAJBgNV\nBAYTAkFVMRMwEQYDVQQIEwpTb21lLVN0YXRlMSEwHwYDVQQKExhJbnRlcm5ldCBX\naWRnaXRzIFB0eSBMdGQwHhcNMTQwNTAyMjEzMTM4WhcNMjQwNTAxMjEzMTM4WjBF\nMQswCQYDVQQGEwJBVTETMBEGA1UECBMKU29tZS1TdGF0ZTEhMB8GA1UEChMYSW50\nZXJuZXQgV2lkZ2l0cyBQdHkgTHRkMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKB\ngQC0BIBp+pFhdwQo5j/809y9+mlM6GCms9u3rXH/s/3S+0bgB81Sve6TOflE1Cst\nfNxfffpX8LC49bsjpuu8yY4jdl3fdSNBsvyiWV7wSVlp3drlcv0dd4jXr1VTUgb4\nPTU7Kq7F49wry3uJfb9mOMTNTt4DMFQF4YczDhtD7GMCTQIDAQABo4GnMIGkMB0G\nA1UdDgQWBBTeQRH9sNNuuBFg7ESvAyVaTrf5QTB1BgNVHSMEbjBsgBTeQRH9sNNu\nuBFg7ESvAyVaTrf5QaFJpEcwRTELMAkGA1UEBhMCQVUxEzARBgNVBAgTClNvbWUt\nU3RhdGUxITAfBgNVBAoTGEludGVybmV0IFdpZGdpdHMgUHR5IEx0ZIIJAPDLIlBz\nkeCvMAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEFBQADgYEArsYQwqW5qb+LY3tV\nnj8kZug41lCogW9OsjdZLo6v2iLqDUQaw28NF7tJykNHm0arKEG5di/HXntMbG9A\nGa1UKEnOFWpHHfNtX8dxzCO1CvpDUqwnVNKc+An7HrRB0Ccs8GiuRQuI2ROhiZ2t\n3/jYGfAKXGnC4WZCvyz1SHpcKpI=\n-----END CERTIFICATE-----');

--
-- Dumping data for table `idp_remote_ids`
--


--
-- Dumping data for table `keyczar_metadata_rax`
--

INSERT INTO `keyczar_metadata_rax` VALUES ('meta','2014-11-13','{\"name\":\"sessionId\",\"purpose\":\"DECRYPT_AND_ENCRYPT\",\"type\":\"AES\",\"versions\":[{\"exportable\":false,\"status\":\"PRIMARY\",\"versionNumber\":1},{\"exportable\":false,\"status\":\"ACTIVE\",\"versionNumber\":2}],\"encrypted\":false}');

--
-- Dumping data for table `keyczar_version_rax`
--

INSERT INTO `keyczar_version_rax` VALUES (1,'meta',NULL,'{\"aesKeyString\":\"NSPi6IekAxLHlJpmT7SXsg\",\"hmacKey\":{\"hmacKeyString\":\"NeVqkm1_ojpaOEm5zmi9X29tOHCXUtcHOOGE9puOapA\",\"size\":256},\"mode\":\"CBC\",\"size\":128}');
INSERT INTO `keyczar_version_rax` VALUES (2,'meta',NULL,'{\"aesKeyString\":\"WwIWqM26_FKKhK8rqcjH1Q\",\"hmacKey\":{\"hmacKeyString\":\"tMrQg7WmCD4SFESMe7JibSelESrhRXZb0Ua3k_WBV-A\",\"size\":256},\"mode\":\"CBC\",\"size\":128}');

--
-- Dumping data for table `mapping`
--


--
-- Dumping data for table `migrate_version`
--

INSERT INTO `migrate_version` VALUES ('endpoint_filter','/Users/jorge/.pyenv/versions/keystone-rax/lib/python2.7/site-packages/keystone/contrib/endpoint_filter/migrate_repo',2);
INSERT INTO `migrate_version` VALUES ('endpoint_policy','/Users/jorge/.pyenv/versions/keystone-rax/lib/python2.7/site-packages/keystone/contrib/endpoint_policy/migrate_repo',1);
INSERT INTO `migrate_version` VALUES ('federation','/Users/jorge/.pyenv/versions/keystone-rax/lib/python2.7/site-packages/keystone/contrib/federation/migrate_repo',8);
INSERT INTO `migrate_version` VALUES ('keystone','/Users/jorge/.pyenv/versions/keystone-rax/lib/python2.7/site-packages/keystone/common/sql/migrate_repo',67);
INSERT INTO `migrate_version` VALUES ('oauth1','/Users/jorge/.pyenv/versions/keystone-rax/lib/python2.7/site-packages/keystone/contrib/oauth1/migrate_repo',5);
INSERT INTO `migrate_version` VALUES ('revoke','/Users/jorge/.pyenv/versions/keystone-rax/lib/python2.7/site-packages/keystone/contrib/revoke/migrate_repo',2);

--
-- Dumping data for table `mobile_phone_rax`
--

INSERT INTO `mobile_phone_rax` VALUES ('fb6c00852e404263b745419dd761d968','+1 201-555-0104','+1 201-555-0104','DP1GRI1GJU52GWCOI2N6');

--
-- Dumping data for table `mobile_phone_user_membership_rax`
--


--
-- Dumping data for table `otp_device_rax`
--

INSERT INTO `otp_device_rax` VALUES ('8ac7a67c1a5549bcb5153ca85615affc','173190',0,'OTP_disabled','DrMez8Af7v6ZLFGX6+Ig6BOobBA=');
INSERT INTO `otp_device_rax` VALUES ('ff39ddbcede641d4b3aacd56e79545ce','057f1b3e8811403d973814e5ed1b41bb',1,'OTP_enabled','Tz5ekJV4LX102qkSzJxxajsKuic=');

--
-- Dumping data for table `pattern_rax`
--

INSERT INTO `pattern_rax` VALUES ('email','^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[A-Za-z]+','Expecting valid email address','');
INSERT INTO `pattern_rax` VALUES ('password','^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])[a-zA-Z\\d=+`|\\(){}\\[\\]:;\"\'<>,.?/\\\\\\\\£~!@#%^&$*_ -]{8,}$','Password must be at least 8 characters in length, must contain at least one uppercase letter, one lowercase letter, and one numeric character.','Checks for invalid characters on a phone number.');
INSERT INTO `pattern_rax` VALUES ('phone','[0-9]*','Phone number has invalid characters.','Checks for invalid characters on a phone number.');
INSERT INTO `pattern_rax` VALUES ('token','^[A-Za-z0-9-]+$','Invalid token','Checks for invalid characters on a token.');
INSERT INTO `pattern_rax` VALUES ('username','^[A-Za-z0-9][a-zA-Z0-9-_.@]*','Username must begin with an alphanumeric character, have no spaces, and only contain the following valid special characters: - _ . @','AlphaNumeric w/ some special characters (- | _ | @ | .)');

--
-- Dumping data for table `policy`
--

INSERT INTO `policy` VALUES ('100000000','type','blob','{}');

--
-- Dumping data for table `policy_association`
--


--
-- Dumping data for table `policy_endpoint_rax`
--

INSERT INTO `policy_endpoint_rax` VALUES ('2de8d9b78ce743f9a9d82a87ee19db51','3043');
INSERT INTO `policy_endpoint_rax` VALUES ('3f242a5d284b419db40f64d4db622cd9','1039');
INSERT INTO `policy_endpoint_rax` VALUES ('3f242a5d284b419db40f64d4db622cd9','1041');
INSERT INTO `policy_endpoint_rax` VALUES ('81a37718897c4ef6a3412ad0878ed12e','3043');
INSERT INTO `policy_endpoint_rax` VALUES ('c8717d9a59c4495092ae041862298011','1039');
INSERT INTO `policy_endpoint_rax` VALUES ('c8717d9a59c4495092ae041862298011','1041');

--
-- Dumping data for table `policy_rax`
--

INSERT INTO `policy_rax` VALUES ('100000000','name',0,0,'description');

--
-- Dumping data for table `project`
--

INSERT INTO `project` VALUES ('78543988','78543988','{}',NULL,1,'78543988',NULL);
INSERT INTO `project` VALUES ('78543995','78543995','{}',NULL,1,'78543995',NULL);
INSERT INTO `project` VALUES ('983452','983452','{}',NULL,1,'983452',NULL);
INSERT INTO `project` VALUES ('MossoCloudFS_78543995','MossoCloudFS_78543995','{}',NULL,1,'78543995',NULL);
INSERT INTO `project` VALUES ('StagingUS_ed361067-aebd-45b7-92a3-353edacb0e09','StagingUS_ed361067-aebd-45b7-92a3-353edacb0e09','{}',NULL,1,'983452',NULL);

--
-- Dumping data for table `project_endpoint`
--

INSERT INTO `project_endpoint` VALUES ('10','MossoCloudFS_78543995');
INSERT INTO `project_endpoint` VALUES ('10','StagingUS_ed361067-aebd-45b7-92a3-353edacb0e09');
INSERT INTO `project_endpoint` VALUES ('1013','MossoCloudFS_78543995');
INSERT INTO `project_endpoint` VALUES ('103','MossoCloudFS_78543995');
INSERT INTO `project_endpoint` VALUES ('103','StagingUS_ed361067-aebd-45b7-92a3-353edacb0e09');
INSERT INTO `project_endpoint` VALUES ('1041','78543988');
INSERT INTO `project_endpoint` VALUES ('1041','78543995');
INSERT INTO `project_endpoint` VALUES ('1042','78543988');
INSERT INTO `project_endpoint` VALUES ('1042','78543995');
INSERT INTO `project_endpoint` VALUES ('109','78543988');
INSERT INTO `project_endpoint` VALUES ('109','78543995');
INSERT INTO `project_endpoint` VALUES ('109','983452');
INSERT INTO `project_endpoint` VALUES ('11','MossoCloudFS_78543995');
INSERT INTO `project_endpoint` VALUES ('11','StagingUS_ed361067-aebd-45b7-92a3-353edacb0e09');
INSERT INTO `project_endpoint` VALUES ('111','MossoCloudFS_78543995');
INSERT INTO `project_endpoint` VALUES ('111','StagingUS_ed361067-aebd-45b7-92a3-353edacb0e09');
INSERT INTO `project_endpoint` VALUES ('120','78543988');
INSERT INTO `project_endpoint` VALUES ('120','78543995');
INSERT INTO `project_endpoint` VALUES ('120','983452');
INSERT INTO `project_endpoint` VALUES ('122','78543988');
INSERT INTO `project_endpoint` VALUES ('122','78543995');
INSERT INTO `project_endpoint` VALUES ('122','983452');
INSERT INTO `project_endpoint` VALUES ('135','78543988');
INSERT INTO `project_endpoint` VALUES ('135','78543995');
INSERT INTO `project_endpoint` VALUES ('135','983452');
INSERT INTO `project_endpoint` VALUES ('15','78543988');
INSERT INTO `project_endpoint` VALUES ('15','78543995');
INSERT INTO `project_endpoint` VALUES ('15','983452');
INSERT INTO `project_endpoint` VALUES ('17','78543988');
INSERT INTO `project_endpoint` VALUES ('17','78543995');
INSERT INTO `project_endpoint` VALUES ('17','983452');
INSERT INTO `project_endpoint` VALUES ('3','MossoCloudFS_78543995');
INSERT INTO `project_endpoint` VALUES ('3','StagingUS_ed361067-aebd-45b7-92a3-353edacb0e09');
INSERT INTO `project_endpoint` VALUES ('47','78543988');
INSERT INTO `project_endpoint` VALUES ('47','78543995');
INSERT INTO `project_endpoint` VALUES ('47','983452');
INSERT INTO `project_endpoint` VALUES ('56','78543988');
INSERT INTO `project_endpoint` VALUES ('56','78543995');
INSERT INTO `project_endpoint` VALUES ('56','983452');
INSERT INTO `project_endpoint` VALUES ('83','78543988');
INSERT INTO `project_endpoint` VALUES ('83','78543995');
INSERT INTO `project_endpoint` VALUES ('83','983452');

--
-- Dumping data for table `project_endpoint_group`
--


--
-- Dumping data for table `project_endpoint_rax`
--

INSERT INTO `project_endpoint_rax` VALUES ('103','MossoCloudFS_78543995');
INSERT INTO `project_endpoint_rax` VALUES ('103','StagingUS_ed361067-aebd-45b7-92a3-353edacb0e09');
INSERT INTO `project_endpoint_rax` VALUES ('111','MossoCloudFS_78543995');
INSERT INTO `project_endpoint_rax` VALUES ('111','StagingUS_ed361067-aebd-45b7-92a3-353edacb0e09');
INSERT INTO `project_endpoint_rax` VALUES ('120','78543988');
INSERT INTO `project_endpoint_rax` VALUES ('120','78543995');
INSERT INTO `project_endpoint_rax` VALUES ('120','983452');
INSERT INTO `project_endpoint_rax` VALUES ('15','78543988');
INSERT INTO `project_endpoint_rax` VALUES ('15','78543995');
INSERT INTO `project_endpoint_rax` VALUES ('15','983452');

--
-- Dumping data for table `property_rax`
--

INSERT INTO `property_rax` VALUES ('encryptionVersionId','0');

--
-- Dumping data for table `question_rax`
--


--
-- Dumping data for table `racker_rax`
--


--
-- Dumping data for table `region`
--

INSERT INTO `region` VALUES ('DFW','',NULL,'{}');
INSERT INTO `region` VALUES ('IAD','',NULL,'{}');
INSERT INTO `region` VALUES ('LON','',NULL,'{}');
INSERT INTO `region` VALUES ('ORD','',NULL,'{}');

--
-- Dumping data for table `region_rax`
--

INSERT INTO `region_rax` VALUES ('DFW',1,'US',0);
INSERT INTO `region_rax` VALUES ('IAD',1,'US',0);
INSERT INTO `region_rax` VALUES ('LON',1,'UK',1);
INSERT INTO `region_rax` VALUES ('ORD',1,'US',1);

--
-- Dumping data for table `request_token`
--


--
-- Dumping data for table `revocation_event`
--


--
-- Dumping data for table `role`
--

INSERT INTO `role` VALUES ('1','identity:admin','{}');
INSERT INTO `role` VALUES ('10','Super Admin','{}');
INSERT INTO `role` VALUES ('10010175','nova:observer','{}');
INSERT INTO `role` VALUES ('10010176','nova:creator','{}');
INSERT INTO `role` VALUES ('10053953','Role 100','{}');
INSERT INTO `role` VALUES ('10053954','Role 500','{}');
INSERT INTO `role` VALUES ('10053955','Role 1000','{}');
INSERT INTO `role` VALUES ('10053956','Role 2000','{}');
INSERT INTO `role` VALUES ('11','Customer Admin','{}');
INSERT INTO `role` VALUES ('122776','rbacRole2','{}');
INSERT INTO `role` VALUES ('2','identity:default','{}');
INSERT INTO `role` VALUES ('22776','rbacRole1','{}');
INSERT INTO `role` VALUES ('249','rax_managed','{}');
INSERT INTO `role` VALUES ('250','rack_connect','{}');
INSERT INTO `role` VALUES ('3','identity:user-admin','{}');
INSERT INTO `role` VALUES ('4','identity:service-admin','{}');
INSERT INTO `role` VALUES ('5','object-store:default','{}');
INSERT INTO `role` VALUES ('6','compute:default','{}');
INSERT INTO `role` VALUES ('7','identity:user-manage','{}');
INSERT INTO `role` VALUES ('8','identity:multifactor_beta','{}');
INSERT INTO `role` VALUES ('9','Racker','{}');

--
-- Dumping data for table `role_rax`
--

INSERT INTO `role_rax` VALUES ('1','Admin Role.',100,0,'bde1268ebabeeabb70a0e702a4626977c331d5c4');
INSERT INTO `role_rax` VALUES ('10','A role that allows a user/application full access to Global Auth',0,0,'18e7a7032733486cd32f472d7bd58f709ac0d221');
INSERT INTO `role_rax` VALUES ('10010175','Nova Observer Role for Account User',1000,0,'bde1268ebabeeabb70a0e702a4626977c331d5c4');
INSERT INTO `role_rax` VALUES ('10010176','Nova Creator Role for Account User',1000,0,'bde1268ebabeeabb70a0e702a4626977c331d5c4');
INSERT INTO `role_rax` VALUES ('10053953','Role for testing',100,0,'bde1268ebabeeabb70a0e702a4626977c331d5c4');
INSERT INTO `role_rax` VALUES ('10053954','Role for testing',500,0,'bde1268ebabeeabb70a0e702a4626977c331d5c4');
INSERT INTO `role_rax` VALUES ('10053955','Role for testing',1000,0,'bde1268ebabeeabb70a0e702a4626977c331d5c4');
INSERT INTO `role_rax` VALUES ('10053956','Role for testing',2000,0,'bde1268ebabeeabb70a0e702a4626977c331d5c4');
INSERT INTO `role_rax` VALUES ('11','A Role that allows a user to Admin other users in his customer',1000,0,'18e7a7032733486cd32f472d7bd58f709ac0d221');
INSERT INTO `role_rax` VALUES ('122776','A random rbac role part II',1000,0,'bde1268ebabeeabb70a0e702a4626977c331d5c4');
INSERT INTO `role_rax` VALUES ('2','Default Role.',2000,0,'bde1268ebabeeabb70a0e702a4626977c331d5c4');
INSERT INTO `role_rax` VALUES ('22776','A random rbac role',1000,0,'bde1268ebabeeabb70a0e702a4626977c331d5c4');
INSERT INTO `role_rax` VALUES ('249','Rackspace Managed Cloud Customer',500,1,'bde1268ebabeeabb70a0e702a4626977c331d5c4');
INSERT INTO `role_rax` VALUES ('250','Rackspace RackConnect Customer',500,1,'bde1268ebabeeabb70a0e702a4626977c331d5c4');
INSERT INTO `role_rax` VALUES ('3','User Admin Role.',750,0,'bde1268ebabeeabb70a0e702a4626977c331d5c4');
INSERT INTO `role_rax` VALUES ('4','Super Admin Role.',0,0,'bde1268ebabeeabb70a0e702a4626977c331d5c4');
INSERT INTO `role_rax` VALUES ('5','A Role that allows a user access to keystone Service methods',500,1,'6047d506862b81d6d99273b4853adfe81e0478c1');
INSERT INTO `role_rax` VALUES ('6','A Role that allows a user access to keystone Service methods',500,1,'a45b14e394a57e3fd4e45d59ff3693ead204998b');
INSERT INTO `role_rax` VALUES ('7','User manage role',900,0,'bde1268ebabeeabb70a0e702a4626977c331d5c4');
INSERT INTO `role_rax` VALUES ('8','Multi-Factor BETA',500,0,'bde1268ebabeeabb70a0e702a4626977c331d5c4');
INSERT INTO `role_rax` VALUES ('9','Defines a user as being a Racker',100,0,'18e7a7032733486cd32f472d7bd58f709ac0d221');

--
-- Dumping data for table `sensitive_config`
--


--
-- Dumping data for table `service`
--

INSERT INTO `service` VALUES ('6047d506862b81d6d99273b4853adfe81e0478c1','object-store',1,'{}');
INSERT INTO `service` VALUES ('6ae999552a0d2dca14d62e2bc8b764d377b1dd6c','identity',0,'{}');
INSERT INTO `service` VALUES ('a45b14e394a57e3fd4e45d59ff3693ead204998b','compute',1,'{}');
INSERT INTO `service` VALUES ('bde1268ebabeeabb70a0e702a4626977c331d5c4','identity',1,'{}');

--
-- Dumping data for table `service_provider`
--


--
-- Dumping data for table `service_rax`
--

INSERT INTO `service_rax` VALUES ('6047d506862b81d6d99273b4853adfe81e0478c1',0);
INSERT INTO `service_rax` VALUES ('6ae999552a0d2dca14d62e2bc8b764d377b1dd6c',0);
INSERT INTO `service_rax` VALUES ('a45b14e394a57e3fd4e45d59ff3693ead204998b',0);
INSERT INTO `service_rax` VALUES ('bde1268ebabeeabb70a0e702a4626977c331d5c4',0);

--
-- Dumping data for table `token`
--


--
-- Dumping data for table `trust`
--


--
-- Dumping data for table `trust_role`
--


--
-- Dumping data for table `user`
--

INSERT INTO `user` VALUES ('057f1b3e8811403d973814e5ed1b41bb','test_4','{\"email\": \"test_4@email.com\"}','{SSHA512}4eJyLTZ/q3Ibtd2ad0ZpUAN+gog2zoBon0e5SGyOeOaz6DoGrCDZh6Sg8bJmqRJyWVUhaT0JKVuz2aupTXvDU8qU2p0=',1,'586db555db044584b48a8cd4fa4e713d',NULL);
INSERT INTO `user` VALUES ('10043250','testServiceAdmin_doNotDelete','{\"email\": \"testEmail@rackspace.com\"}','{SSHA512}ZuZlB4PP4+n9NlfGU4OzpPYE6ppk2gdXbmfwYxzRsjwQB+herqBQM+GQh0zF39vfNVZ7HaRCh2FcCNQsRFHn05TRwGE=',1,'cf7237733bcb4c78840b3600ee2026b9',NULL);
INSERT INTO `user` VALUES ('10043490','testDisabledUser_doNotDelete','{\"email\": \"testEmail@rackspace.com\"}','{SSHA512}0Y1ljhv5vrj2r3wizyBeTKt0Tt2ifleO1wvg7qGmp8D8hbQXoAjl2qjAS9G/AyBcoNN+lt42t69gbIcv0gHi0YRSgp0=',0,'6e395680dc074f1f89248331efc68720',NULL);
INSERT INTO `user` VALUES ('10044477','testIdentityAdmin_doNotDelete','{\"email\": \"testEmail@rackspace.com\"}','{SSHA512}ZhX+o/hPkLOFEIZY+gxVxLxRAr8uYGE2rPdIU+dahKSXaBEIVlEQzWTNtUSe/5VliMzdNGFA5jM8W41Qy5CQp+rBaAs=',1,'f5b7cc5f541b44c1872257db85b46f73',NULL);
INSERT INTO `user` VALUES ('10044550','testUserAdmin_doNotDelete','{\"email\": \"testuser@example.org\"}','{SSHA512}+4bRt0YNOASnVPfHO41Syy6+wpZfTGnun3EohLSc3eMZILVSHjYtCMCm98SCHcH/K6NeXOiGwcwMpKXLjceuZqm7x78=',1,'135792468',NULL);
INSERT INTO `user` VALUES ('10044551','testUserNoRoles_doNotDelete','{\"email\": \"testEmail@rackspace.com\"}','{SSHA512}OCjh52ZoTwZUVj3vtygRSwPlJpY8fkFtWmFKCd2QQwuz5mI/WzPKqA8vO4CJBnuJ/0Hwf2tq9Uk1jMxxmNeQpVz4MjY=',1,'135792468',NULL);
INSERT INTO `user` VALUES ('10046995','testDefaultUser_doNotDelete','{\"email\": \"testEmail@rackspace.com\"}','{SSHA512}BARDd98YVBXutNsQ/iazStgL3f2gsEmamVLhk28KZwGAWKXurbk9S9K9uq+iUjZcVbvLdzjH+PoXPpBsivtQh3nAy0s=',1,'135792468',NULL);
INSERT INTO `user` VALUES ('10071819','cloud10User_DoNotDelete','{}','{SSHA512}ZJUOznNJYQS5wra1sP22f+6IxQFarW8NarR1dm49egSLXkYcJZPtxjVFssf3egSw1giTfqsCUcnogn5kPdsmka+F+ZY=',1,'-543569094',NULL);
INSERT INTO `user` VALUES ('173189','auth','{\"email\": \"auth@rackspace.com\"}','{SSHA512}RdvDpwL42wU7mx6kf+g9vFD30Ks2i0lsiRWf/iCLg39UCN3rye5BIol2jUZc2frXcVwrDYZTi/F/cPiOFcG6o0AvDOo=',1,'0e37c9edd8934c6991d065d8d2930bf2',NULL);
INSERT INTO `user` VALUES ('173190','authQE','{\"email\": \"auth@rackspace.com\"}','{SSHA512}RAb1t3VMMTzb3fQmfJszhFdPniIM9PEsxyrnBkVqB6tVc8ayXMgWdCLkYPeDbCA/YQygpcB8VyvRhsnNzHNO9XpI/dY=',1,'8432e98010984f6498cba7c99930fe97',NULL);
INSERT INTO `user` VALUES ('173195','authQE2','{\"email\": \"auth@rackspace.com\"}','{SSHA512}ccYiEd2iEh9Bcr+pYT9o9M1B1EGg8TzsiJaPBfuQmwzwH/coAeFA9qElrGXUA1yazRWMmEUYdj+dlxLn8Kh/+dTQE04=',1,'d041b82df2a04e1f9c8393e96834cc70',NULL);
INSERT INTO `user` VALUES ('3a4407a6b3e349998478409c5b7eda14','cloudCafeUserAdmin','{}','{SSHA512}aV7cSVt0C2Es2efBtEI7U7gVhlN+zS6APHlaKh2rGJqgb1J7jgcLlmEqm8Cc5ffWzmDYjUxnEbejNwfA5CCvNQOBPU0=',1,'983452','983452');
INSERT INTO `user` VALUES ('4e997592aad24e2183e51bd013f223c5','keystone_user_admin','{}','{SSHA512}0d2ldlhL6wuErFZYqazvZnAbyN87geeZO8+DBTBz/MivZnK5xUt0EZ1s4Z/9FpPk95yinR9KBYzJzH8uLjkUcHL5R30=',1,'78543988','78543988');
INSERT INTO `user` VALUES ('d14b1195cbe045d9905c25317dd1f924','cloudIdentitySA1','{\"email\": \"new_email_1_614979@test.com\"}','{SSHA512}3ezkZfEsxKeRKztpWvSuEU/1MKEv1D0nNVnccGSQyYLTJ/5btnOZf4bM1R5eQeWYOxIg2joiKSlE4mUI2tsREy0cPCY=',1,'c403172f0b8f42679414315a991820b0',NULL);
INSERT INTO `user` VALUES ('d8f1b8df14c844d9919aee4e19a2f6f9','keystone_mfa_user_admin','{}','{SSHA512}6NmWxePZPI101gxL3FuyoEDE95yAWvHPQOodY9/EYmx5L3MO1BOAA8hxTC59kvOMgIm3MmBylzFxoxhNHvXN8ATq9+g=',1,'78543995','78543995');
INSERT INTO `user` VALUES ('e2655b97cfe848b796097c2073b174e7','cloudIdentitySA2','{\"email\": \"new_email_2_814159@example.com\"}','{SSHA512}k1xmvdFrf5WRgmZobev5WZXuF3uGRx15hE5ZKYCX3HTEluP2JfHQFmdSIq1wuAfgd1PE8FZ+X+pcgIHI5KrwtFuzrwQ=',1,'382a1c2b9b05417bb00af8155ddc3b64',NULL);

--
-- Dumping data for table `user_certificate_rax`
--

INSERT INTO `user_certificate_rax` VALUES ('2f16c3eb7e43433a8791c2655a8a96d1','test',0x308202B030820219A003020102020900F0CB22507391E0AF300D06092A864886F70D01010505003045310B3009060355040613024155311330110603550408130A536F6D652D53746174653121301F060355040A1318496E7465726E6574205769646769747320507479204C7464301E170D3134303530323231333133385A170D3234303530313231333133385A3045310B3009060355040613024155311330110603550408130A536F6D652D53746174653121301F060355040A1318496E7465726E6574205769646769747320507479204C746430819F300D06092A864886F70D010101050003818D0030818902818100B4048069FA9161770428E63FFCD3DCBDFA694CE860A6B3DBB7AD71FFB3FDD2FB46E007CD52BDEE9339F944D42B2D7CDC5F7DFA57F0B0B8F5BB23A6EBBCC98E23765DDF752341B2FCA2595EF0495969DDDAE572FD1D7788D7AF55535206F83D353B2AAEC5E3DC2BCB7B897DBF6638C4CD4EDE03305405E187330E1B43EC63024D0203010001A381A73081A4301D0603551D0E04160414DE4111FDB0D36EB81160EC44AF03255A4EB7F94130750603551D23046E306C8014DE4111FDB0D36EB81160EC44AF03255A4EB7F941A149A4473045310B3009060355040613024155311330110603550408130A536F6D652D53746174653121301F060355040A1318496E7465726E6574205769646769747320507479204C7464820900F0CB22507391E0AF300C0603551D13040530030101FF300D06092A864886F70D010105050003818100AEC610C2A5B9A9BF8B637B559E3F2466E838D650A8816F4EB237592E8EAFDA22EA0D441AC36F0D17BB49CA43479B46AB2841B9762FC75E7B4C6C6F4019AD542849CE156A471DF36D5FC771CC23B50AFA4352AC2754D29CF809FB1EB441D0272CF068AE450B88D913A1899DADDFF8D819F00A5C69C2E16642BF2CF5487A5C2A92);
INSERT INTO `user_certificate_rax` VALUES ('694d8518511049d3ba5450a1601c8418','identityqe',0x3082039F30820308A0030201020209009D5DADD222420EC4300D06092A864886F70D0101050500308192310B3009060355040613027573310E300C060355040813057465786173311430120603550407130B73616E20616E746F6E696F31123010060355040A13097261636B7370616365310E300C060355040B1305636C6F7564310C300A06035504031303716533312B302906092A864886F70D010901161C7368616E7468692E766564756C61407261636B73706163652E636F6D301E170D3134303830373136333230385A170D3234303830363136333230385A308192310B3009060355040613027573310E300C060355040813057465786173311430120603550407130B73616E20616E746F6E696F31123010060355040A13097261636B7370616365310E300C060355040B1305636C6F7564310C300A06035504031303716533312B302906092A864886F70D010901161C7368616E7468692E766564756C61407261636B73706163652E636F6D30819F300D06092A864886F70D010101050003818D0030818902818100AB29BA714965D485223EE503348C78236B20214B308D30EC036F3BE4BA5437DDF9AE5C4441E79316C6F1FBC638CE534A4E48F9E492FF0F7AC2F5AD68E21D09355E98541349BC4C9C5B11D2983F4C4AE323D687F65623CCC4026D1F79644CA46E4588A86C3709DC302E6AC8E6FBDA99C0FF321C04204650E0265E5A448EEB314D0203010001A381FA3081F7301D0603551D0E04160414F06BBCEC1581D9D78551CE92CCECC8442209D1613081C70603551D230481BF3081BC8014F06BBCEC1581D9D78551CE92CCECC8442209D161A18198A48195308192310B3009060355040613027573310E300C060355040813057465786173311430120603550407130B73616E20616E746F6E696F31123010060355040A13097261636B7370616365310E300C060355040B1305636C6F7564310C300A06035504031303716533312B302906092A864886F70D010901161C7368616E7468692E766564756C61407261636B73706163652E636F6D8209009D5DADD222420EC4300C0603551D13040530030101FF300D06092A864886F70D01010505000381810039F0155FD4297715FF7F33F2183068BD978D330A154DC9F9E07FAAF48BAE4CBDD24AA4D4F211B64822F51281CE5A1706201F5FB42A2EDD4C218F7E86EF42C7CF124228D61D784BCF5FF4102C74082B989880DCE823727C35DEAA72D613163D960CE442CB04D780644759449DDDCAE13BC3F809E46548B833714A7F4CC911430A);
INSERT INTO `user_certificate_rax` VALUES ('761704fdc9cb497180cba6165b015146','identityqe',0x3082039F30820308A003020102020900DDB8940B2C9A557F300D06092A864886F70D0101050500308192310B3009060355040613027573310E300C060355040813057465786173311430120603550407130B73616E20616E746F6E696F31123010060355040A13097261636B7370616365310E300C060355040B1305636C6F7564310C300A06035504031303716532312B302906092A864886F70D010901161C7368616E7468692E766564756C61407261636B73706163652E636F6D301E170D3134303830373136333133375A170D3234303830363136333133375A308192310B3009060355040613027573310E300C060355040813057465786173311430120603550407130B73616E20616E746F6E696F31123010060355040A13097261636B7370616365310E300C060355040B1305636C6F7564310C300A06035504031303716532312B302906092A864886F70D010901161C7368616E7468692E766564756C61407261636B73706163652E636F6D30819F300D06092A864886F70D010101050003818D0030818902818100D7C1011D36618D40FB4EF476AC1925A28734D5F0CBBEF2AA7A535B9C2AAE940E91F77BE1EFE4633AB763712BE4339B2B597FFA002698D6EB75C97B005765746478BD4AE17B9F6AE9CD352B958A816D82B2B106B9861D2BA041F6457DC8078C2B3BEB35014CFDB681737D71CD278BD0C1FBB798B261BD3C193897865D611435410203010001A381FA3081F7301D0603551D0E041604146357F27650E086B55803C1D5DDAA88FFBE84B4953081C70603551D230481BF3081BC80146357F27650E086B55803C1D5DDAA88FFBE84B495A18198A48195308192310B3009060355040613027573310E300C060355040813057465786173311430120603550407130B73616E20616E746F6E696F31123010060355040A13097261636B7370616365310E300C060355040B1305636C6F7564310C300A06035504031303716532312B302906092A864886F70D010901161C7368616E7468692E766564756C61407261636B73706163652E636F6D820900DDB8940B2C9A557F300C0603551D13040530030101FF300D06092A864886F70D0101050500038181009494A52091CF3B6C9C7C3D229CEA5AD252BF98FBDFD5B31B9B631007C7E96026D4AE5EA339F9FA41E87B6DFD20E110339F214FC8BD8121A30EEA5D19CAFCDF4DBB7A3B87B187060C6CB9E05D4AF4DD17881FC42B31545D0E466AF4636574A4CC2D2EB98D7108EEBDA01813F8DA328436BCA033924A262DC37FBAB32CC3D95478);
INSERT INTO `user_certificate_rax` VALUES ('8c33853f6b5b499ab9c0beacbee9bf97','identityqe',0x30820390308202F9A003020102020900E24FF2B2FBC11E28300D06092A864886F70D010105050030818D310B3009060355040613027573310E300C060355040813057465786173311430120603550407130B73616E20616E746F6E696F31123010060355040A13097261636B7370616365310E300C060355040B1305636C6F7564310B30090603550403130271653127302506092A864886F70D01090116187368616E7468692E766564756C6140676D61696C2E636F6D301E170D3134303731343231333835305A170D3234303731333231333835305A30818D310B3009060355040613027573310E300C060355040813057465786173311430120603550407130B73616E20616E746F6E696F31123010060355040A13097261636B7370616365310E300C060355040B1305636C6F7564310B30090603550403130271653127302506092A864886F70D01090116187368616E7468692E766564756C6140676D61696C2E636F6D30819F300D06092A864886F70D010101050003818D0030818902818100ACD84C17C4ED199F9E319DC86619E69B4EB0D7E5F00F1A52FDF0E0A4D9328B5B6DA1572B5950C18CDF27ECF290ACEB15D02E014D6E93244B83BE2B07034E23C304A99385EAD6CFDD532B2623137F718D16DDE78BF85212551C41D323A2E81A3ECB75A73938762F4DEF31DE251B328CA20554D303C88A428FF3744B24E25B40A70203010001A381F53081F2301D0603551D0E0416041498AC8995C0DB8501FB79F923B7D95541B32663093081C20603551D230481BA3081B7801498AC8995C0DB8501FB79F923B7D95541B3266309A18193A4819030818D310B3009060355040613027573310E300C060355040813057465786173311430120603550407130B73616E20616E746F6E696F31123010060355040A13097261636B7370616365310E300C060355040B1305636C6F7564310B30090603550403130271653127302506092A864886F70D01090116187368616E7468692E766564756C6140676D61696C2E636F6D820900E24FF2B2FBC11E28300C0603551D13040530030101FF300D06092A864886F70D0101050500038181007C39B099E8DC10022FA1A86869DAAD4E4D4B8D756CF86EC8C87D93BA150A8A3C4C10DFC91D46C0D8F2C77B1D1FB5276CA6C0050511521CF7AE5ABD32EDF3B1F146D36D089DFC6246D3C085051459BFC370B0A306B4C7F26A441479B14D0358744E14D40EFBBF32F1AA05C4D3AE3534AAFEEB15EF6299E6261BFEF23F3F3DE973);
INSERT INTO `user_certificate_rax` VALUES ('91fc45cbd4584b749a1da32c2eeec289','dedicated',0x308201F53082019FA003020102020900CA6E2FCAF7A5E09B300D06092A864886F70D01010505003056310B3009060355040613025553310E300C06035504080C0554657861733114301206035504070C0B53616E20416E746F6E696F3121301F060355040A0C18496E7465726E6574205769646769747320507479204C7464301E170D3133303730393232323734395A170D3134303730393232323734395A3056310B3009060355040613025553310E300C06035504080C0554657861733114301206035504070C0B53616E20416E746F6E696F3121301F060355040A0C18496E7465726E6574205769646769747320507479204C7464305C300D06092A864886F70D0101010500034B003048024100B4B4BDCFCA53F13E3D5AB8C37608D588682B2A3DCCB82CAF4EBA48F149F5247B03A2EA42A8BFD692DA97B402C0BCBB09213E3ECF97EC34955632192AC0879F0F0203010001A350304E301D0603551D0E0416041408F04CE0F4CF2F9D7A3B8ADDBC9D45BAC58F7BE2301F0603551D2304183016801408F04CE0F4CF2F9D7A3B8ADDBC9D45BAC58F7BE2300C0603551D13040530030101FF300D06092A864886F70D01010505000341003A8F14A40FA5166167FC35F8944812858A5CA26D753BE292DE67FC349865DBFDF8C0BC34209D2DD41A7AF9429E10B5155D46BC7A9B5A42E267B23EB131B6B2C7);
INSERT INTO `user_certificate_rax` VALUES ('ed1aac91bfcc4668b817e6186f7c192d','identityqe',0x3082039F30820308A003020102020900F80368ED17A6047A300D06092A864886F70D0101050500308192310B3009060355040613027573310E300C060355040813057465786173311430120603550407130B73616E20616E746F6E696F31123010060355040A13097261636B7370616365310E300C060355040B1305636C6F7564310C300A06035504031303716534312B302906092A864886F70D010901161C7368616E7468692E766564756C61407261636B73706163652E636F6D301E170D3134303830373136333235375A170D3234303830363136333235375A308192310B3009060355040613027573310E300C060355040813057465786173311430120603550407130B73616E20616E746F6E696F31123010060355040A13097261636B7370616365310E300C060355040B1305636C6F7564310C300A06035504031303716534312B302906092A864886F70D010901161C7368616E7468692E766564756C61407261636B73706163652E636F6D30819F300D06092A864886F70D010101050003818D0030818902818100B820506913F2FFB298182DFBCF0611AE8ED00C5A32B44EA8A01C6AEF7981F19B74762EACA1D4377DA30082B0914EEA49665A35CBAD14429C41894D9A89DAF866910879ACFFA2A8650A985E260DABCDE47A496274B664272C42FF09C92EC6EE6B058538D7D2EB49A7DEC6CC18B18C95A589B40E567628EB1EA81F42CF219550570203010001A381FA3081F7301D0603551D0E0416041437EC84A1FC0DF3464A1CFF0B76F1FFCC0B1519983081C70603551D230481BF3081BC801437EC84A1FC0DF3464A1CFF0B76F1FFCC0B151998A18198A48195308192310B3009060355040613027573310E300C060355040813057465786173311430120603550407130B73616E20616E746F6E696F31123010060355040A13097261636B7370616365310E300C060355040B1305636C6F7564310C300A06035504031303716534312B302906092A864886F70D010901161C7368616E7468692E766564756C61407261636B73706163652E636F6D820900F80368ED17A6047A300C0603551D13040530030101FF300D06092A864886F70D0101050500038181007A3C58E1C863985EB11738103E625972D7298B1CCD567A9A458B2A0FC45383E03E4538EC939E74CCFAD270A75D70B6150C9F7444C68992F8CE8B4E08F75AAE0D839090BDD49C7C9C09753D82D864A7A288CBA67A4C31166F82F0ED16BFBFE971B81F2CDC16F7F5BF8515E3EEEFFAF2A59D1B619E50103FF46E8F11DC273E4BC7);

--
-- Dumping data for table `user_group_membership`
--


--
-- Dumping data for table `user_rax`
--

INSERT INTO `user_rax` VALUES ('057f1b3e8811403d973814e5ed1b41bb',NULL,NULL,'2015-05-20',0,'0',NULL,NULL,NULL,NULL,'lieutwd0MOj/hkyIfyVTt3GTHGXvoB9JXar/0TteScxx09fOPo97v8cfnDkR8Okn',NULL,'ORD',NULL,NULL,NULL,'3d c7 89 dd 60 22 a6 8c 66 7b fe 93 e8 10 e5 24',NULL,NULL,NULL,NULL,0,1,NULL,'ACTIVE',NULL,'OTP',NULL,NULL);
INSERT INTO `user_rax` VALUES ('10043250',NULL,NULL,'2012-09-14',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2015-03-23','2015-03-23',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,NULL,NULL,NULL,NULL,NULL);
INSERT INTO `user_rax` VALUES ('10043490',NULL,NULL,'2012-09-18',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2015-03-23','2015-03-23',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,NULL,NULL,NULL,NULL,NULL);
INSERT INTO `user_rax` VALUES ('10044477',NULL,NULL,'2012-09-20',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2015-03-23','2015-03-23',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,NULL,NULL,NULL,NULL,NULL);
INSERT INTO `user_rax` VALUES ('10044550',NULL,NULL,'2012-09-24',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2015-03-23',NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,NULL,NULL,NULL,NULL,NULL);
INSERT INTO `user_rax` VALUES ('10044551',NULL,NULL,'2012-09-24',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2015-03-23',NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,NULL,NULL,NULL,NULL,NULL);
INSERT INTO `user_rax` VALUES ('10046995',NULL,NULL,'2012-10-12',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2015-03-23',NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,NULL,NULL,NULL,NULL,NULL);
INSERT INTO `user_rax` VALUES ('10071819',NULL,NULL,'2013-03-22',0,NULL,NULL,NULL,NULL,NULL,'Is2/L0NNUM3svnnjOcEqVg==','StagingUS_e99825fd-b069-4987-bf46-42766bf79fd3','ORD','2015-03-23',NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,NULL,NULL,NULL,NULL,NULL);
INSERT INTO `user_rax` VALUES ('173189','18e7a7032733486cd32f472d7bd58f709ac0d221',NULL,'2012-09-19',0,NULL,'C5WykLHFtUz6bdFvapQ3pw==','L0EuPN+S7sRR739Wqqhv6g==',NULL,'F/J7NK9Jv2H4w2Xg2xuJjQ==','cBoF/yUr7bFU8DrFOGNiEw==','MossoCloudFS_6eee84c5-54a4-4217-a895-8308da81feb3','ORD','2015-03-23','2015-03-23',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,NULL,NULL,NULL,NULL,NULL);
INSERT INTO `user_rax` VALUES ('173190','18e7a7032733486cd32f472d7bd58f709ac0d221',NULL,'2012-03-12',0,NULL,'C5WykLHFtUz6bdFvapQ3pw==','L0EuPN+S7sRR739Wqqhv6g==',NULL,'F/J7NK9Jv2H4w2Xg2xuJjQ==',NULL,'MossoCloudFS_6eee84c5-54a4-4217-a895-8308da81feb3','ORD','2015-03-23','2015-03-23',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,NULL,NULL,NULL,NULL,NULL);
INSERT INTO `user_rax` VALUES ('173195','18e7a7032733486cd32f472d7bd58f709ac0d221',NULL,'2012-03-12',0,NULL,'C5WykLHFtUz6bdFvapQ3pw==','L0EuPN+S7sRR739Wqqhv6g==',NULL,'F/J7NK9Jv2H4w2Xg2xuJjQ==',NULL,NULL,'ORD','2015-03-23','2015-03-23',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,NULL,NULL,NULL,NULL,NULL);
INSERT INTO `user_rax` VALUES ('3a4407a6b3e349998478409c5b7eda14',NULL,NULL,'2013-11-07',0,'0','KqVIjuJ4KSJdTahVxy+yw0lcP4kzcwuizjYfX7ciwOy+QKjv9heiA+bVF7j7EvzZdFjU9Bdmh3S+qHUWGp1hEw==','Uv1LGk0mZpLhrbYoASrOeg==','gOtvzwQZ4QhZIQ/PnebrYg==',NULL,'2/DFjhASk0f9SBMdaW457jLCoO7nxvvwDjiSmHDSbUkymcryUT6U0yWGOJVGB6Kw','StagingUS_ed361067-aebd-45b7-92a3-353edacb0e09','ORD','2015-03-23',NULL,NULL,'e3 1b 06 4c 50 3f 7e ed 91 2f f7 91 47 ae 93',NULL,NULL,NULL,NULL,0,0,NULL,NULL,NULL,NULL,NULL,NULL);
INSERT INTO `user_rax` VALUES ('4e997592aad24e2183e51bd013f223c5',NULL,NULL,'2014-10-06',0,'0',NULL,NULL,NULL,NULL,'iG9M5xFRpttxIeTvbx1aiwI95c5/IYJ8I4ZKShvIwXqPN+D7dhoNzdkm2KTbZ6VH','MossoCloudFS_78543988','ORD',NULL,NULL,NULL,'10 4a 89 d2 7d 47 b7 bc 42 82 79 5b 22 3d 14 7a',NULL,NULL,NULL,NULL,0,0,NULL,NULL,NULL,NULL,NULL,NULL);
INSERT INTO `user_rax` VALUES ('d14b1195cbe045d9905c25317dd1f924',NULL,NULL,'2013-12-13',0,'0',NULL,NULL,NULL,'jKeMVeagD/fCkftKqz3iJq9mfnk3JxxXYwHNmcY8dPY=','QTCMTLYLBEn2lBMluNU+cjRqk3NFT4l3k1Gk9Fx6B+xFe2lC0fzPyvINLufUhOQO',NULL,'ORD','2015-03-23','2015-03-23',NULL,'e7 2d 3f 76 ec 8a c9 21 5e 99 15 40 83 76 f9',NULL,NULL,NULL,NULL,0,0,NULL,NULL,NULL,NULL,NULL,NULL);
INSERT INTO `user_rax` VALUES ('d8f1b8df14c844d9919aee4e19a2f6f9',NULL,NULL,'2015-01-15',0,NULL,NULL,NULL,NULL,NULL,'cBoF/yUr7bFU8DrFOGNiEw==','MossoCloudFS_78543995','ORD','2015-03-23',NULL,NULL,NULL,NULL,'fb6c00852e404263b745419dd761d968',NULL,NULL,1,1,'DUMEPCV67EA3I30G0TBM','ACTIVE',NULL,NULL,NULL,NULL);
INSERT INTO `user_rax` VALUES ('e2655b97cfe848b796097c2073b174e7',NULL,NULL,'2013-12-13',0,'0',NULL,NULL,NULL,'F1z2ahKSquHcDr3Zid+LnULuVb50Z1o/+BSeTvrf/wo=','s8GwNbai8iFtjP7M7K8LcBt4Tbl3ZtVd8Dj7YSffflHsMR74vx4uEfm2A2F+L9JM',NULL,'ORD','2015-03-23','2015-03-23',NULL,'1a 0b 8c 92 88 dd df 7c 4c 68 e8 00 77 7e 07 6d',NULL,NULL,NULL,NULL,0,0,NULL,NULL,NULL,NULL,NULL,NULL);

--
-- Dumping data for table `whitelisted_config`
--

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2015-07-16 16:24:10
