package com.rackspace.idm.domain.security.keystone

import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.domain.security.DefaultAETokenService
import com.rackspace.idm.domain.security.DefaultAETokenServiceBaseIntegrationTest
import com.rackspace.idm.domain.security.UnmarshallTokenException
import org.apache.commons.lang.StringUtils
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

class V3ProvisionedUserTokenIntegrationTest extends DefaultAETokenServiceBaseIntegrationTest {
    /**
     * curl -X POST http://localhost:35357/v3/auth/tokens -H "content-type:application/json" -d '{"auth": {"identity": {"methods": ["password"],"password": {"user": {"id": "10046995","password": "Password1"}}}}}' -v | python -mjson.tool
     *
     * {
     "token": {
     "audit_ids": [
     "yoQM1MneR3WfrDuZU3QcgQ"
     ],
     "expires_at": "2015-09-09T03:52:43.976138Z",
     "extras": {},
     "issued": "2015-09-09T03:37:43.976166Z",
     "methods": [
     "password"
     ],
     "user": {
     "domain": {
     "id": "135792468",
     "name": "135792468"
     },
     "id": "10046995",
     "name": "testDefaultUser_doNotDelete"
     }
     }
     }
     * @return
     */
    def provisionedUserUnscopedToken = "AQD1Pcpjp2rYEWXPIhGH7Y_7XOIYbi81UY6E9vgzZqJDh5LGJeT721EPQK-8o_FGuB_xCzthD-MXCKu1vsqD4jYtdLQ-6ERe8J-caP9LcdmJ9pA0gsUl0lB8ddIZemUj8E0K3QZRWmjslA"


    /**
     * curl -X POST http://localhost:35357/v3/auth/tokens -H "content-type:application/json" -d '{"auth": {"identity": {"methods": ["password"],"password": {"user": {"id": "4e997592aad24e2183e51bd013f223c5","password": "Auth1234"}}},"scope": {"domain": {"id": "78543988"}}}}' -v | python -mjson.tool
     *
     {
     "token": {
     "audit_ids": [
     "rIiO_mPiQSKOm60GRzQQdQ"
     ],
     "catalog": [
     {
     "endpoints": [
     {
     "id": "28b3759c8a274f3cadcaf800a2721605",
     "interface": "admin",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://preprod.dfw.images.api.rackspacecloud.com/v1"
     },
     {
     "id": "37dde4cebfb24a1ea57aa96283b1ed4e",
     "interface": "public",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://"
     }
     ],
     "id": "02aec197e76448b6b3730725ccbc2c3c",
     "name": "cloudImagesPreprod",
     "type": "undefined"
     },
     {
     "endpoints": [
     {
     "id": "574a2c97118a4ab4a1b9554f731ca32c",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://"
     },
     {
     "id": "705a1f0d00e34f38ba343a4a339c42ca",
     "interface": "admin",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://staging.customer.api.rackspacecloud.com/v1/customer_accounts"
     },
     {
     "id": "7814faf2f29b4d9d9422c26c754d3d24",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://"
     },
     {
     "id": "7c05d0f4d48b4c8587e5da9f254c7342",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://"
     },
     {
     "id": "91a9452ecd434af388fd20dbd703a4ea",
     "interface": "admin",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://customer.api.rackspacecloud.com/v1/customer_accounts"
     },
     {
     "id": "bab00899b56940b0b09d40d6d644cea8",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://"
     },
     {
     "id": "dcb7bed2272f49ef957bd2951b1aef64",
     "interface": "admin",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://customer.api.rackspacecloud.com/v1/customer_accounts"
     },
     {
     "id": "e2bbcf1c4b52402ca150f9c81f85e6ed",
     "interface": "admin",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://staging.customer.api.rackspacecloud.com/v1/customer_accounts/CLOUD"
     }
     ],
     "id": "09ca2c9b733e4ce38a31116f9e94c020",
     "name": "CustomerService",
     "type": "rax:customer"
     },
     {
     "endpoints": [],
     "id": "18e7a7032733486cd32f472d7bd58f709ac0d221",
     "name": "Customer Idm",
     "type": "identity"
     },
     {
     "endpoints": [
     {
     "id": "558519bc68a04c0dac5dbd9d534f915f",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "http://qe.ord.lunr.racklabs.com/v1"
     },
     {
     "id": "bd36b27de44a4dbfb56ae8f5e8492f2c",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://qe.ord.lunr.racklabs.com/v1"
     }
     ],
     "id": "20f6cc8ab08c45e39ca153218f22280d",
     "name": "cloudBlockStorageQE",
     "type": "volume"
     },
     {
     "endpoints": [
     {
     "id": "3f052fcc9f3a4ac68fd9954a74941190",
     "interface": "admin",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://service-admin.com/v1"
     },
     {
     "id": "bcd02e366ee2496f82232b7bbf1366fc",
     "interface": "internal",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://service-internal.com/v1 "
     },
     {
     "id": "cad952b3151d49f9a340ebb1406b25b1",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://service-public.com/v1 "
     }
     ],
     "id": "23f724d4470b4c32899127939f911113",
     "name": "Compute",
     "type": "compute"
     },
     {
     "endpoints": [
     {
     "id": "1404fd1bc59749d18c41328cf8c078ff",
     "interface": "admin",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://staging.billing.api.rackspacecloud.com/v1/accounts"
     },
     {
     "id": "420f921fbdcf41f79a62d32c1e1386b4",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://"
     },
     {
     "id": "4856ab4eb1e64508b5df8c67efb4efb9",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://"
     },
     {
     "id": "6dd51e3d250940cd8da6a04fe4298345",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://"
     },
     {
     "id": "ac271a1631c2481a966a26da425ebf42",
     "interface": "admin",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://staging.billing.api.rackspacecloud.com/v1/accounts"
     },
     {
     "id": "d5e31752d9874842a72c92b7f151a164",
     "interface": "admin",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://billing.api.rackspacecloud.com/v1/accounts"
     }
     ],
     "id": "2673429b4a72498088392e838c962b5f",
     "name": "BillingService",
     "type": "rax:biling"
     },
     {
     "endpoints": [
     {
     "id": "1e69263e6ae24052a3c3fd374c55e673",
     "interface": "public",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://dfw.loadbalancers.api.rackspacecloud.com/v1.0"
     },
     {
     "id": "4444694e2ce7423a84e859c29a4d37f2",
     "interface": "public",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://dfw.loadbalancers.api.rackspacecloud.com/v1.0"
     },
     {
     "id": "6cc8e55efbaa45469d09ba2fa6ba0b07",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://ord.loadbalancers.api.rackspacecloud.com/v1.0"
     },
     {
     "id": "a88fdebfdc414597b500e0864a18b722",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://staging.lon3.loadbalancers.api.rackspacecloud.com/v1.0"
     },
     {
     "id": "aa43fb0cddb045698a231f43e325b6ec",
     "interface": "public",
     "region": "LON",
     "region_id": "LON",
     "url": "https://lon.loadbalancers.api.rackspacecloud.com/v1.0"
     },
     {
     "id": "c9ff67b8d7ba4e2db114bb9ba53c4c40",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://ord.loadbalancers.api.rackspacecloud.com/v1.0"
     },
     {
     "id": "cfc6b22957fb4ed3b7d229d65cea1bc9",
     "interface": "public",
     "region": "LON",
     "region_id": "LON",
     "url": "https://lon.loadbalancers.api.rackspacecloud.com/v1.0"
     },
     {
     "id": "ef02ed30fc1540f8b3f45371863b57e0",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://staging.ord.loadbalancers.api.rackspacecloud.com/v1.0"
     }
     ],
     "id": "3b0b5431f4e846438fbcfd8f1f893629",
     "name": "cloudLoadBalancers",
     "type": "rax:load-balancer"
     },
     {
     "endpoints": [
     {
     "id": "24dfcf11b52f47748664faeb19ea54df",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://"
     },
     {
     "id": "2abeb17d0a60450793e38ae828761b8e",
     "interface": "public",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://"
     },
     {
     "id": "30c50ae197a945338841dd7d48a5cabf",
     "interface": "admin",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://ord.images.api.rackspacecloud.com/v1"
     },
     {
     "id": "7f9456b48141401f8c85d07caf2a5811",
     "interface": "admin",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://dfw.images.api.rackspacecloud.com/v1"
     },
     {
     "id": "a0516a36a2de4edaaf4283beb5788c87",
     "interface": "admin",
     "region": "LON",
     "region_id": "LON",
     "url": "https://lon.images.api.rackspacecloud.com/v1"
     },
     {
     "id": "a64c66b9146f40589c8b398d3412438f",
     "interface": "public",
     "region": "LON",
     "region_id": "LON",
     "url": "https://"
     }
     ],
     "id": "3b324c9c30ba4f2a8d2f8e14e2cea5f0",
     "name": "cloudImages",
     "type": "undefined"
     },
     {
     "endpoints": [
     {
     "id": "95f7ab80590449aa9ffff68bbb238f91",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "http://volume-api.cbsbeta.ord1.ohthree.com/v1"
     }
     ],
     "id": "406983bf5a4248c9ba3a430c764be912",
     "name": "cloudVolumeCBSPreview",
     "type": "volume"
     },
     {
     "endpoints": [
     {
     "id": "15a36a8466ee47719713168fb9a40fde",
     "interface": "admin",
     "region": "DFW",
     "region_id": "DFW",
     "url": "http://10.24.31.26:9292/v1/images"
     },
     {
     "id": "6e76d90d6e9a420580b40b40b7160852",
     "interface": "public",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://preprod.dfw.images.api.rackspacecloud.com/v1"
     },
     {
     "id": "f5474c7539e3491badbb8f8cb98ec89a",
     "interface": "public",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://"
     }
     ],
     "id": "41779496f06c4cd4a4ae9d60655a3b2c",
     "name": "cloudImagesPublicPreprod",
     "type": "image"
     },
     {
     "endpoints": [
     {
     "id": "108a1bab129d47f9ba9e22fee13fbd13",
     "interface": "internal",
     "region": "ORD",
     "region_id": "ORD",
     "url": "http://10.127.7.19/pods/"
     },
     {
     "id": "9e47e18ceef04873b32a6df686bd3d37",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "http://10.127.7.19/pods/"
     }
     ],
     "id": "4864112ffb4f49fe9de5ad3ce50eba8f",
     "name": "rax:whales",
     "type": "rax:whales"
     },
     {
     "endpoints": [
     {
     "id": "13e919cf114244359a2c99f60837422e",
     "interface": "admin",
     "region": "LON",
     "region_id": "LON",
     "url": "https://lon.images.api.rackspacecloud.com/v1"
     },
     {
     "id": "4e53d24f41bb40f4b7bc320e606755c2",
     "interface": "admin",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://ord.images.api.rackspacecloud.com/v1"
     },
     {
     "id": "5055c39bc5204eb28d8b53a176128a91",
     "interface": "public",
     "region": "LON",
     "region_id": "LON",
     "url": "https://"
     },
     {
     "id": "603a72013fb442b3b06276b5d6240fe9",
     "interface": "public",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://"
     },
     {
     "id": "8910feba2b22402397303821a4dd9295",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://"
     },
     {
     "id": "8ee035f9263d4c319326efa3f5b099db",
     "interface": "admin",
     "region": "LON",
     "region_id": "LON",
     "url": "https://lon.images.api.rackspacecloud.com/v1"
     },
     {
     "id": "9fd7da2f05564fb5b95fb00c1f121ecb",
     "interface": "admin",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://ord.images.api.rackspacecloud.com/v1"
     },
     {
     "id": "a36d594691ed41d6b0f421b584189d14",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://"
     },
     {
     "id": "aea24f66cc584ad083a40f14fb70f4c1",
     "interface": "admin",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://dfw.images.api.rackspacecloud.com/v1"
     },
     {
     "id": "ed5a94b63bcd4e528086466ca4e5de60",
     "interface": "admin",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://dfw.images.api.rackspacecloud.com/v1"
     },
     {
     "id": "ee079d917f0e46359135b554ff2063e5",
     "interface": "public",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://"
     },
     {
     "id": "f5d4738bbb914299930961097515405a",
     "interface": "public",
     "region": "LON",
     "region_id": "LON",
     "url": "https://"
     }
     ],
     "id": "4fb10470a0e1423b80848ac6e115b4ec",
     "name": "cloudImages",
     "type": "image"
     },
     {
     "endpoints": [
     {
     "id": "35b0bd1018814a7881127b177bd23d07",
     "interface": "admin",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://preprod.dfw.images.api.rackspacecloud.com/v1"
     },
     {
     "id": "b5fd53ccf5924aad886498c602e888af",
     "interface": "public",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://"
     },
     {
     "id": "ead77728cf77400f91ef2fe11ca6ab1e",
     "interface": "public",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://"
     },
     {
     "id": "f6832c29ddb84f2299986ed693dbb9ba",
     "interface": "admin",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://staging.preprod.dfw.images.api.rackspacecloud.com/v1"
     }
     ],
     "id": "500095b376214c78bd53e3397e8e538b",
     "name": "cloudImagesPreprod",
     "type": "image"
     },
     {
     "endpoints": [
     {
     "id": "4f7165fd634a4a2ea883e16520493e05",
     "interface": "public",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://dfw.servers.api.rackspacecloud.com/v2"
     },
     {
     "id": "78b2d054ca4b4754992c8425dfe34464",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://ord.servers.api.rackspacecloud.com/v2"
     },
     {
     "id": "b5363eaf43b541e1a07da81fdd67bf82",
     "interface": "public",
     "region": "LON",
     "region_id": "LON",
     "url": "https://lon.servers.api.rackspacecloud.com/v2"
     }
     ],
     "id": "502bef771ca24744a948596bcb99530c",
     "name": "cloudServersOpenStack",
     "type": "undefined"
     },
     {
     "endpoints": [
     {
     "id": "1557e86fe4f74e758a1bd65a0a1bbb65",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://public/"
     },
     {
     "id": "5df2ae50c5b24c0e876bb0792380823e",
     "interface": "admin",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://admin-mix/"
     },
     {
     "id": "66b536bf76444b66a017af07ff5a305b",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://public/"
     },
     {
     "id": "779a0c304c22440087a31898469b2b16",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://public/"
     },
     {
     "id": "950e2665575d497a9feeb060989cef0d",
     "interface": "admin",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://admin-mix/"
     },
     {
     "id": "b24192b38e244d7dbdd6343a59e318ff",
     "interface": "admin",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://admin-mix/"
     }
     ],
     "id": "5cae4c279ff54aeb8589d5fdc24a48df",
     "name": "admin-Mix",
     "type": "undefined"
     },
     {
     "endpoints": [
     {
     "id": "09167b30924441648fdbf9b02d6248a9",
     "interface": "internal",
     "region": "LON",
     "region_id": "LON",
     "url": "https://snet-storage.stg.swift.racklabs.com/v1"
     },
     {
     "id": "11f9a5590eed4068b4430b96782cbc9b",
     "interface": "internal",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://snet-storage101.dfw1.stg.swift.racklabs.com/v1/v1"
     },
     {
     "id": "1be53909f4af46f881ee7ca7070d770f",
     "interface": "internal",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://snet-storage.stg.swift.racklabs.com/v1"
     },
     {
     "id": "1d8ab38d191b453586f65cfc9ba3352e",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://storage.stg.swift.racklabs.com/v1/"
     },
     {
     "id": "3165e848b1a1462198cd441a4ea01427",
     "interface": "internal",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://snet-storage101.dfw1.stg.swift.racklabs.com/v1"
     },
     {
     "id": "39b4dc7a6554417f859454af0227a9b7",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://otherstorage.clouddrive.com/v1"
     },
     {
     "id": "3ef8f99eac584af392ec8b1971e7c894",
     "interface": "internal",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://snet-storage.stg.swift.racklabs.com/v1"
     },
     {
     "id": "57a29b9b15ef4054aaf98d96874acf50",
     "interface": "internal",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://snet-storage.stg.swift.racklabs.com/v1"
     },
     {
     "id": "641ecff9609d4d89a31324381430f706",
     "interface": "internal",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://snet-storage101.dfw1.stg.swift.racklabs.com"
     },
     {
     "id": "80cb889d7a9444f98b5d8137026010fd",
     "interface": "public",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://storage.stg.swift.racklabs.com/v1"
     },
     {
     "id": "85efa96f38fd4000ad12b67b1aa39e4e",
     "interface": "internal",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://otherstorage-snet.clouddrive.com/v1"
     },
     {
     "id": "899b5cd8d8e8478b86cadd167ab001d7",
     "interface": "public",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://storage101.dfw1.stg.swift.racklabs.com/v1/v1"
     },
     {
     "id": "9017f573b2ad427abead57d5f99a2eb2",
     "interface": "public",
     "region": "LON",
     "region_id": "LON",
     "url": "https://storage.stg.swift.racklabs.com/v1/"
     },
     {
     "id": "950fbb7812824b84a341869166279ca9",
     "interface": "internal",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://snet-storage.stg.swift.racklabs.com/v1"
     },
     {
     "id": "956488b911ff44b19b617b0a1e763679",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://storage.stg.swift.racklabs.com/v1"
     },
     {
     "id": "9cd1736881cf4de5a31615d4b6f86936",
     "interface": "public",
     "region": "LON",
     "region_id": "LON",
     "url": "https://storage.stg.swift.racklabs.com/v1"
     },
     {
     "id": "aa4b2ef528ac4a1a9bc46b0190fdc64c",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://otherstorage.clouddrive.com/v1"
     },
     {
     "id": "b0e3eaa774cb47949dbcf612f4ca9aa4",
     "interface": "internal",
     "region": "LON",
     "region_id": "LON",
     "url": "https://snet-storage.stg.swift.racklabs.com/v1"
     },
     {
     "id": "bd8d661dec2e44289ac473216b7da831",
     "interface": "public",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://storage.stg.swift.racklabs.com/v1"
     },
     {
     "id": "d0630dccce7b4194a7c771113892c9ad",
     "interface": "internal",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://snet-storage.stg.swift.racklabs.com/v1"
     },
     {
     "id": "dd3109714d46428588f0a817963e7e3d",
     "interface": "internal",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://otherstorage-snet.clouddrive.com/v1"
     },
     {
     "id": "e08c0c298c8945c5a12049766f080279",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://storage.stg.swift.racklabs.com/v1"
     },
     {
     "id": "e42b51f4950145d6b72d39e248859265",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://storage.stg.swift.racklabs.com/v1"
     },
     {
     "id": "f0a40ead88be4e4abccc13e58dae1288",
     "interface": "admin",
     "region": "ORD",
     "region_id": "ORD",
     "url": "http://someadmin.com/v1"
     },
     {
     "id": "f81393ca160349e4b79865041880f73b",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://storage.stg.swift.racklabs.com/v1"
     }
     ],
     "id": "6047d506862b81d6d99273b4853adfe81e0478c1",
     "name": "cloudFiles",
     "type": "object-store"
     },
     {
     "endpoints": [
     {
     "id": "8965ff4032424db089f8438eeafb15cf",
     "interface": "public",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://preprod.dfw.servers.api.rackspacecloud.com/v2"
     }
     ],
     "id": "613ab2dd9e314ae0854933286cd4c8c1",
     "name": "cloudServersPreprod",
     "type": "undefined"
     },
     {
     "endpoints": [
     {
     "id": "c9dc27efe34f4864af8be849b93fd124",
     "interface": "public",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://dfw.blockstorage.api.rackspacecloud.com/v1"
     }
     ],
     "id": "6598b4f12aa64adab079f346125ad425",
     "name": "cloudBlockStorage",
     "type": "volume"
     },
     {
     "endpoints": [
     {
     "id": "44ebe3e5e2d94d678d62d8e2e8633a9f",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://preprod.ord.servers.api.rackspacecloud.com/v2"
     },
     {
     "id": "a2fdeeb9680a4e168e5eb43bedfb1600",
     "interface": "public",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://preprod.dfw.servers.api.rackspacecloud.com/v2"
     },
     {
     "id": "cdbc99bfba034981aa9acd00f5cb417a",
     "interface": "public",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://preprod.dfw.servers.api.rackspacecloud.com/v2"
     }
     ],
     "id": "70cd7894ac644f83bef3e8fe97684581",
     "name": "cloudServersPreprod",
     "type": "compute"
     },
     {
     "endpoints": [
     {
     "id": "0b1b17f2ae8d496f80168c05cd53c34a",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://"
     },
     {
     "id": "2688a685db584972915a5d9df61dd0ba",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://"
     },
     {
     "id": "2dc8c0e040a8437ba5e06690e6c7f70d",
     "interface": "admin",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://staging.incident.api.rackspacecloud.com/v1"
     },
     {
     "id": "32cc924547a648d3a2688345a92e5347",
     "interface": "admin",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://incident.api.rackspacecloud.com/v1"
     },
     {
     "id": "74b99c7beadc45ee88da9772fe161708",
     "interface": "admin",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://staging.incident.api.rackspacecloud.com/v1"
     },
     {
     "id": "8e449d136dc64affa05b37fcb38d8828",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://"
     }
     ],
     "id": "72a4100ec2a641399a3938fba7141c69",
     "name": "IncidentService",
     "type": "rax:incident"
     },
     {
     "endpoints": [
     {
     "id": "0ac26aa5bc7c4c22a47420226d7f7af8",
     "interface": "public",
     "region": "LON",
     "region_id": "LON",
     "url": "https://lon.servers.api.rackspacecloud.com/v2"
     },
     {
     "id": "78e7eaa7026e439c977488ffe667a692",
     "interface": "public",
     "region": "LON",
     "region_id": "LON",
     "url": "http://alpha.lon.servers.api.rackspacecloud.com:8774/v1.1"
     },
     {
     "id": "79888d889c914016a5716261b3c508b6",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "http://alpha.ord.servers.api.rackspacecloud.com:8774/v1.1"
     },
     {
     "id": "79db7ffff19340de9b2aa53be92c0cd6",
     "interface": "public",
     "region": "LON",
     "region_id": "LON",
     "url": "https://admin.lon.servers.api.rackspacecloud.com/v2"
     },
     {
     "id": "859c89691c254438b493a6107c9a5b43",
     "interface": "public",
     "region": "LON",
     "region_id": "LON",
     "url": "https://lon.servers.api.rackspacecloud.com/v2"
     },
     {
     "id": "8a910c370c0d479bb0084ca926d030a3",
     "interface": "admin",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://incident.api.rackspacecloud.com/v1"
     },
     {
     "id": "8b116a422d0c40d48630956d348c5cdd",
     "interface": "public",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://dfw.servers.api.rackspacecloud.com/v2"
     },
     {
     "id": "95e4107a8b6e4c75adaaeea51daa1400",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://preprod.ord.servers.api.rackspacecloud.com/v2"
     },
     {
     "id": "a16c39f6a7d54b3d977d96bc8b675163",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://ord.servers.api.rackspacecloud.com/v2"
     },
     {
     "id": "a207bae52199419ab6fe4b63b8262713",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://admin.ord.servers.api.rackspacecloud.com/v2"
     },
     {
     "id": "a4f536f68fa34d0e8879be6b9693c893",
     "interface": "public",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://preprod.dfw.servers.api.rackspacecloud.com/v2"
     },
     {
     "id": "a61bdb23ab2c446ca72f416fe3465530",
     "interface": "admin",
     "region": "LON",
     "region_id": "LON",
     "url": "https://usage.api.rackspacecloud.com/v1"
     },
     {
     "id": "a6392cd91c794edeb1fdd7e3b2c5263f",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "http://alpha-test.ord.servers.api.rackspacecloud.com:8774/v1.1"
     },
     {
     "id": "ae4b4bca664e4cacbe29f574e174f330",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "http://alpha-test.lon.servers.api.rackspacecloud.com:8774/v1.1"
     },
     {
     "id": "ff8d0846cdbf49418eb8be6d93eaf93f",
     "interface": "admin",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://billing.api.rackspacecloud.com/v1/accounts"
     }
     ],
     "id": "778280ee70664371a0100a1938d8dafe",
     "name": "cloudServersOpenStack",
     "type": "compute"
     },
     {
     "endpoints": [
     {
     "id": "1158d4bc2f404b0cb9daa95d3446c0cc",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "http://nova-api1.a0001.h48.ohthree.com:8774/v2"
     },
     {
     "id": "162374335854430db9a52dd172434001",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "http://api:8774/v2"
     },
     {
     "id": "43ed6e5f7be74fd6b6e8288a3cec7825",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "http://public.url"
     },
     {
     "id": "5640a65f850d492cad35b8dc78a5dccd",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "http://api:8774/v2"
     },
     {
     "id": "94453aa03d3b46c291089077010213ad",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "http://api:8774/v2"
     },
     {
     "id": "f8a818bc9ef145949ad88fc04a4f3678",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "http://nova-api1.a0001.h48.ohthree.com:8774/v2"
     }
     ],
     "id": "79964ea548f747f2932ade27a4692b7a",
     "name": "openstackHuddle48",
     "type": "compute"
     },
     {
     "endpoints": [
     {
     "id": "1937cca04a0f47a585e55d2d09d6222b",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://ord.databases.api.rackspacecloud.com/v1.0"
     },
     {
     "id": "e06042d8a48845afbd38f61d436466fa",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://api.staging.ord1.clouddb.rackspace.net/v1.0"
     },
     {
     "id": "eadf130630204d8aadb6e2c18917efdf",
     "interface": "public",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://dfw.databases.api.rackspacecloud.com/v1.0"
     }
     ],
     "id": "7cc417f495c64430828309588dad7393",
     "name": "cloudDatabases",
     "type": "rax:database"
     },
     {
     "endpoints": [
     {
     "id": "b4674493aca04b8bbeb544252ba56d0b",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://preview-ord.servers.api.rackspacecloud.com/v2"
     }
     ],
     "id": "810955c3e53342a3b8a72034e4c312d1",
     "name": "cloudServersOpenStackPreview",
     "type": "compute"
     },
     {
     "endpoints": [
     {
     "id": "17d621258d154e3a8141cad447fb6b10",
     "interface": "admin",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://usage.api.rackspacecloud.com/v1"
     },
     {
     "id": "83b7875b3bde4a958e6f7e01d1373c98",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://"
     },
     {
     "id": "a3374993031546f9b3205725e60113f8",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://"
     },
     {
     "id": "c63b32a69c5b4d7ba256ac1c361fc5d9",
     "interface": "admin",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://staging.usage.api.rackspacecloud.com/v1"
     },
     {
     "id": "c9590666315247f8acab5a0794f1bbe4",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://"
     },
     {
     "id": "eb6b43dd0c4444148563931d8bd998fe",
     "interface": "admin",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://staging.usage.api.rackspacecloud.com/v1"
     }
     ],
     "id": "8bcc248d64274b758871d6982fb3bd3b",
     "name": "UsageService",
     "type": "rax:usage"
     },
     {
     "endpoints": [
     {
     "id": "5e728418dc7a49379a9c1b1245460220",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://ord.servers.preview.api.rackspacecloud.com/v2"
     }
     ],
     "id": "92aec25366af45b58adef9329f8b9da3",
     "name": "cloudServersPreview",
     "type": "compute"
     },
     {
     "endpoints": [
     {
     "id": "048ae13e61f048e28c7bd1495b906f7d",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://cmstaging.api.rackspacecloud.com/v1.0"
     },
     {
     "id": "29c8b540a3664c63be7b72c74c9ad553",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://staging.monitoring.api.rackspacecloud.com/v1.0"
     },
     {
     "id": "3f70e9b0d9e64045b301727ef6b41e64",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://staging.monitoring.api.rackspacecloud.com/v1.0"
     },
     {
     "id": "5041efbc8b484cfca580bd46f6159f7b",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://monitoring.api.rackspacecloud.com/v1.0"
     },
     {
     "id": "f733052a932e41d2b0fe07de1fb14ae5",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://monitoring.api.rackspacecloud.com/v1.0"
     }
     ],
     "id": "92f71b49fc774e3093808e30d9b999cb",
     "name": "cloudMonitoring",
     "type": "rax:monitor"
     },
     {
     "endpoints": [
     {
     "id": "404f093d07294c48993eb73c3acbeeb1",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://admin.dfw.servers.api.rackspacecloud.com/v2"
     }
     ],
     "id": "959f12e90eb14309ac43f5b102dd9a1f",
     "name": "cloudServersPreprodAdmin",
     "type": "compute"
     },
     {
     "endpoints": [
     {
     "id": "209b1f3b1265402981a871576025a97b",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "http://volume-api:8774/v1"
     },
     {
     "id": "4153af43e9d742329e4b03e2834cbefb",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "http://volume-api:8774/v1"
     },
     {
     "id": "7d2cf7f269214e708667e2c2b63bfd2f",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "http://volume-api1.a0001.h48.ohthree.com:8774/v1"
     }
     ],
     "id": "9db754ac79c2438a872b3386faeb6822",
     "name": "cloudBlockStorageHuddle48",
     "type": "volume"
     },
     {
     "endpoints": [
     {
     "id": "2019ebb4320d47cf9edd196f5bcc160c",
     "interface": "public",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://cdn.stg.clouddrive.com/v1"
     },
     {
     "id": "525796e2249b4fd88c8f3fd0faeebb48",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://cdn.stg.clouddrive.com/v1"
     },
     {
     "id": "59d9242951584cb5a63522de001f5878",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://cdn.stg.clouddrive.com/v1"
     },
     {
     "id": "62ce8daef46341f4b6e6033b2cffa6f9",
     "interface": "public",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://cdn.stg.clouddrive.com/v1"
     },
     {
     "id": "77285842bdf54034a8caa3d4e049bbfb",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://cdn.stg.clouddrive.com"
     },
     {
     "id": "7ca9341fb62947b4873acfe84622a136",
     "interface": "public",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://cdn.stg.clouddrive.com/v1"
     },
     {
     "id": "7e75542ac6954b64a3e27cebcd8cdded",
     "interface": "public",
     "region": "LON",
     "region_id": "LON",
     "url": "https://cdn.stg.clouddrive.com/v1"
     },
     {
     "id": "995be14c010a4646ba0732b8446abb4c",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://cdn.stg.clouddrive.com/v1"
     },
     {
     "id": "b03ebcdcaaba48bc85d406b8a587fdf2",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://cdn.stg.clouddrive.com/v1"
     },
     {
     "id": "cbafd23257d5468abf339ebf2b53d4c5",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://cdn.stg.clouddrive.com/v1"
     },
     {
     "id": "cddbbab88f674ec1a90638dbe4a1b507",
     "interface": "public",
     "region": "LON",
     "region_id": "LON",
     "url": "https://cdn.stg.clouddrive.com/v1"
     },
     {
     "id": "d598477be3ed452d9dc8d7dfc0d19142",
     "interface": "public",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://cdn.stg.clouddrive.com/v1"
     },
     {
     "id": "d73f843222f94a49a1e58e0b39e317cc",
     "interface": "admin",
     "region": "ORD",
     "region_id": "ORD",
     "url": "http://image:9292/v1"
     }
     ],
     "id": "9e1adf6284ac4599a67a82f3d0aa116e",
     "name": "cloudFilesCDN",
     "type": "rax:object-cdn"
     },
     {
     "endpoints": [
     {
     "id": "57bb3018324f462db7afbe55d5e96de1",
     "interface": "public",
     "region": "LON",
     "region_id": "LON",
     "url": "https://servers.staging.ord1.uk.ccp.rackspace.net/v1.0"
     },
     {
     "id": "5a9cfa60a41f4744add68e53ef042b3a",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://servers.api.staging.us.ccp.rackspace.net/v1.0"
     },
     {
     "id": "6bbf938b274c42b28c31645d6c946894",
     "interface": "public",
     "region": "LON",
     "region_id": "LON",
     "url": "https://ord.servers.api.rackspacecloud.com/v1.0"
     },
     {
     "id": "966a3d2a231a41729c895e1f83127d32",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "http://api-n01.qe.ord1.us.cloudcompute.rackspace.net:8080/v1.0"
     },
     {
     "id": "a474bf0accd74f27bf39fba69b052001",
     "interface": "public",
     "region": "LON",
     "region_id": "LON",
     "url": "https://ord.servers.api.rackspacecloud.com/v1.0/"
     },
     {
     "id": "e77eac6a42354b8997b18f6952cec561",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://servers.api.staging.us.ccp.rackspace.net/v1.0"
     }
     ],
     "id": "a45b14e394a57e3fd4e45d59ff3693ead204998b",
     "name": "cloudServers",
     "type": "compute"
     },
     {
     "endpoints": [
     {
     "id": "99aaec3ad0a54dba9925ed78fbbbb197",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://preprod.ord.blockstorage.api.rackspacecloud.com/v1"
     }
     ],
     "id": "b1b53e09b2cc4b2d96a7a470f7566774",
     "name": "cloudBlockStoragePreprod",
     "type": "volume"
     },
     {
     "endpoints": [
     {
     "id": "42ba1172ad464cb184c51f75e6186c80",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://ord.cbs.preview.api.rackspacecloud.com/v1"
     },
     {
     "id": "d094881670c44d8c872ba21d0926417b",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://preview-ord.blockstorage.api.rackspacecloud.com/v1"
     }
     ],
     "id": "bb234657064a419ba20cc25e86a52cc9",
     "name": "cloudBlockStoragePreview",
     "type": "volume"
     },
     {
     "endpoints": [
     {
     "id": "635f8366a55b40538d2f2bfb45a437d9",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://staging.dnsaas.rackspace.net/v1.0"
     },
     {
     "id": "f1a128d4238945b78a14556f1d503153",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://staging.dnsaas.rackspace.net/v1.0"
     }
     ],
     "id": "bcf53d061ef545149f577fd9682dd3cb",
     "name": "cloudDNS",
     "type": "rax:dns"
     },
     {
     "endpoints": [],
     "id": "bde1268ebabeeabb70a0e702a4626977c331d5c4",
     "name": "Cloud Auth Service",
     "type": "identity"
     },
     {
     "endpoints": [
     {
     "id": "13d0c96215104e95a004635c9f2b751f",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://otherstorage.clouddrive.com/v1"
     },
     {
     "id": "3d3d1f3f033a4c3a8d4fdc4c6f4e2366",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "test"
     },
     {
     "id": "b78e4ede6ae242e493bebb999c378ecb",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "test"
     },
     {
     "id": "d889f0c45912438093ee4f37537872fb",
     "interface": "internal",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://otherstorage-snet.clouddrive.com/v1"
     }
     ],
     "id": "c47fbdd0032e465d8809aee5efe3724b",
     "name": "test",
     "type": "cloudServers"
     },
     {
     "endpoints": [
     {
     "id": "219ba29ea2664560bc89da7123655ea7",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://public-all-urls/"
     },
     {
     "id": "3a632bae6ce74c219bb1580b1d502801",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://public-all-urls/"
     },
     {
     "id": "44bd386d54e547cab7254d7f5d5a7605",
     "interface": "internal",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://internal-all-urls/"
     },
     {
     "id": "5971bce10ed045cdadc45ed5985e41d9",
     "interface": "internal",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://internal-all-urls/"
     },
     {
     "id": "d180494f4bff4bc48b9b361ebb15f799",
     "interface": "admin",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://admin-all-urls/"
     },
     {
     "id": "ebf0b747e3f14f6f8814f00c1b211d2a",
     "interface": "admin",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://admin-all-urls/"
     }
     ],
     "id": "cebeb660339e41fe9cab4ed3ff061a6c",
     "name": "all-urls",
     "type": "undefined"
     },
     {
     "endpoints": [
     {
     "id": "710e7739d5e445f9828ca88aa7775686",
     "interface": "public",
     "region": null,
     "region_id": null,
     "url": "http://localhost:35357/v3"
     },
     {
     "id": "7f5cd7598a744a668b3c1cd51b9b168e",
     "interface": "admin",
     "region": null,
     "region_id": null,
     "url": "http://localhost:35357/v3"
     }
     ],
     "id": "d0bf1ef2b7424314b09b7e491e8b4837",
     "name": "Keystone",
     "type": "identity"
     },
     {
     "endpoints": [
     {
     "id": "13217f94b3d3475588a870ecb93687fa",
     "interface": "admin",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://admin-only/"
     },
     {
     "id": "364c79eca1f446fd8dd89cccdd5764d1",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://"
     },
     {
     "id": "36f945dffd4e46f6b295e5fd06ed1cd1",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://"
     },
     {
     "id": "891e8567854244ec8c56d1e3d14f3b85",
     "interface": "admin",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://admin-only/"
     },
     {
     "id": "df1418a9eeba4002b6e4d39d54b59d5c",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://"
     },
     {
     "id": "f5dc8072c835448f84559cd74a15692b",
     "interface": "admin",
     "region": "ORD",
     "region_id": "ORD",
     "url": "https://admin-only/"
     }
     ],
     "id": "dd4054bb620a4a4ab6d6a8cd10d13c41",
     "name": "admin-Only",
     "type": "undefined"
     },
     {
     "endpoints": [
     {
     "id": "54e99c0d105044bf888672140b07bc17",
     "interface": "public",
     "region": "ORD",
     "region_id": "ORD",
     "url": "http://api.cbsbeta.ord1.ohthree.com/v2"
     }
     ],
     "id": "e07bed78618f4df997a5c1d3020578eb",
     "name": "cloudServersCBSPreview",
     "type": "compute"
     },
     {
     "endpoints": [
     {
     "id": "83d03b75b8fe4e63817e3ff0e25144fa",
     "interface": "admin",
     "region": "DFW",
     "region_id": "DFW",
     "url": "http://image:9292/v1"
     },
     {
     "id": "979a5b10e2bd4c9ba4df191cd8e03fc5",
     "interface": "admin",
     "region": "DFW",
     "region_id": "DFW",
     "url": "http://image:9292/v1"
     },
     {
     "id": "cefa7a5175f04a0b8347f9b01b1137b8",
     "interface": "public",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://"
     },
     {
     "id": "febc6aaacbd54a479c4d201d87f10422",
     "interface": "public",
     "region": "DFW",
     "region_id": "DFW",
     "url": "https://"
     }
     ],
     "id": "e5095fa908084151a4032c3eead377f4",
     "name": "cloudImagesHuddle48",
     "type": "image"
     }
     ],
     "domain": {
     "id": "78543988",
     "name": "78543988"
     },
     "expires_at": "2015-10-09T19:07:18.000000Z",
     "extras": {},
     "issued": "2015-10-09T18:52:18.000000Z",
     "methods": [
     "password"
     ],
     "roles": [
     {
     "id": "6",
     "name": "compute:default"
     }
     ],
     "user": {
     "domain": {
     "id": "78543988",
     "name": "78543988"
     },
     "id": "4e997592aad24e2183e51bd013f223c5",
     "name": "keystone_user_admin"
     }
     }
     }
     * @return
     */
    def provisionedUserDomainScopedToken = "AQD1PcpjyFP3--U4noPkUVZmqh1y2qsR004KNwHToeO3KsQfRoPMRVrrYEQ5XUDjmewMTK5M03Wjs18uNu3U9ZHcxb5ogCPXcSUz8N3xTAR_OtYrKe0VgsoVa0KVLceYj88O4r586jc7cs6t6-1G8CBcwRkeqjXGGhA"

    /**
     *
     * Response from v3 auth:
     *
     * curl -X POST http://localhost:35357/v3/auth/tokens -H "content-type:application/json" -d '{
     "auth": {"identity": {"methods": ["password"],"password": {"user": {"id": "4e997592aad24e2183e51bd013f223c5","password": "Auth1234"}}}}}' -v | python -mjson.tool
     *
     * {
     "token": {
     "audit_ids": [
     "Prwky86ETFajvU9794nK1A"
     ],
     "catalog": [
     ...
     ],
     "expires_at": "2015-09-09T03:31:22.454996Z",
     "extras": {},
     "issued": "2015-09-09T03:16:22.455025Z",
     "methods": [
     "password"
     ],
     "project": {
     "domain": {
     "id": "78543988",
     "name": "78543988"
     },
     "id": "78543988",
     "name": "78543988"
     },
     "roles": [
     {
     "id": "6",
     "name": "compute:default"
     }
     ],
     "user": {
     "domain": {
     "id": "78543988",
     "name": "78543988"
     },
     "id": "4e997592aad24e2183e51bd013f223c5",
     "name": "keystone_user_admin"
     }
     }
     }
     *
     *
     * @return
     */
    def provisionedUserProjectScopedToken = "AQD1PcpjaOfGpQ85NXOLkpGA5nEwpYWllvKPxY1Z-GPEBTWqGPHMyI49Ybjn_ooh58fcjXwmU0qruQB3o5-pyJuYAqdl-bVsYNrLgsV20ubiJewWTNeNZkTteBK5d2TS7B_f0Y2WvIQA6JjUtfclKEo25IVWD7LlVCY"

    def setup() {
        reloadableConfig.setProperty(IdentityConfig.FEATURE_SUPPORT_V3_PROVISIONED_USER_TOKENS_PROP, true)
    }

    def "unmarshall fully populated v3 unscoped user token w/ non-uuid user id"() {
        String expireDateStr = "2015-09-09T03:52:43.976Z"; //note only millisecond precision
        DateTime expectedExpireDate = ISODateTimeFormat.dateTime().parseDateTime(expireDateStr);

        String expectedIssuedDateStr = "2015-09-09T03:37:43.976Z" //note only millisecond precision
        DateTime expectedIssuedDate = ISODateTimeFormat.dateTime().parseDateTime(expectedIssuedDateStr);

        String expectedIssuedToUserId = "10046995"

        when:
        UserScopeAccess unmarshalledScopeAccess = (UserScopeAccess) aeTokenService.unmarshallToken(provisionedUserUnscopedToken)

        then:
        unmarshalledScopeAccess != null
        unmarshalledScopeAccess.accessTokenString == provisionedUserUnscopedToken
        unmarshalledScopeAccess.accessTokenExp.equals(expectedExpireDate.toDate())
        unmarshalledScopeAccess.createTimestamp.equals(expectedIssuedDate.toDate())
        unmarshalledScopeAccess.clientId.equals(identityConfig.getStaticConfig().getCloudAuthClientId())
        unmarshalledScopeAccess.authenticatedBy.size() == 1
        unmarshalledScopeAccess.authenticatedBy.get(0).equals(AuthenticatedByMethodEnum.PASSWORD.getValue())
        unmarshalledScopeAccess.issuedToUserId.equals(expectedIssuedToUserId)

        //unused fields
        StringUtils.isEmpty(unmarshalledScopeAccess.scope)
        StringUtils.isEmpty(unmarshalledScopeAccess.clientRCN)
        unmarshalledScopeAccess.refreshTokenExp == null
        unmarshalledScopeAccess.userPasswordExpirationDate == null
        StringUtils.isEmpty(unmarshalledScopeAccess.refreshTokenString)
        StringUtils.isEmpty(unmarshalledScopeAccess.clientRCN)
    }

   def "unmarshall v3 project scoped user token"() {
        String expireDateStr = "2015-09-09T03:31:22.454Z"; //note only millisecond precision
        DateTime expectedExpireDate = ISODateTimeFormat.dateTime().parseDateTime(expireDateStr);

        String expectedIssuedDateStr = "2015-09-09T03:16:22.455Z" //note only millisecond precision
        DateTime expectedIssuedDate = ISODateTimeFormat.dateTime().parseDateTime(expectedIssuedDateStr);

        String expectedIssuedToUserId = "4e997592aad24e2183e51bd013f223c5"

        when:
        UserScopeAccess unmarshalledScopeAccess = (UserScopeAccess) aeTokenService.unmarshallToken(provisionedUserProjectScopedToken)

        then:
        unmarshalledScopeAccess != null
        unmarshalledScopeAccess.accessTokenString == provisionedUserProjectScopedToken
        unmarshalledScopeAccess.accessTokenExp.equals(expectedExpireDate.toDate())
        unmarshalledScopeAccess.createTimestamp.equals(expectedIssuedDate.toDate())
        unmarshalledScopeAccess.clientId.equals(identityConfig.getStaticConfig().getCloudAuthClientId())
        unmarshalledScopeAccess.authenticatedBy.size() == 1
        unmarshalledScopeAccess.authenticatedBy.get(0).equals(AuthenticatedByMethodEnum.PASSWORD.getValue())
        unmarshalledScopeAccess.issuedToUserId.equals(expectedIssuedToUserId)

        //unused fields
        StringUtils.isEmpty(unmarshalledScopeAccess.scope)
        StringUtils.isEmpty(unmarshalledScopeAccess.clientRCN)
        unmarshalledScopeAccess.refreshTokenExp == null
        unmarshalledScopeAccess.userPasswordExpirationDate == null
        StringUtils.isEmpty(unmarshalledScopeAccess.refreshTokenString)
        StringUtils.isEmpty(unmarshalledScopeAccess.clientRCN)
    }

    def "unmarshall v3 domain scoped user token"() {
        String expireDateStr = "2015-10-09T19:07:18.000000Z"; //note only millisecond precision
        DateTime expectedExpireDate = ISODateTimeFormat.dateTime().parseDateTime(expireDateStr);

        String expectedIssuedDateStr = "2015-10-09T18:52:18.000000Z" //note only millisecond precision
        DateTime expectedIssuedDate = ISODateTimeFormat.dateTime().parseDateTime(expectedIssuedDateStr);

        String expectedIssuedToUserId = "4e997592aad24e2183e51bd013f223c5"

        when:
        UserScopeAccess unmarshalledScopeAccess = (UserScopeAccess) aeTokenService.unmarshallToken(provisionedUserDomainScopedToken)

        then:
        unmarshalledScopeAccess != null
        unmarshalledScopeAccess.accessTokenString == provisionedUserDomainScopedToken
        unmarshalledScopeAccess.accessTokenExp.equals(expectedExpireDate.toDate())
        unmarshalledScopeAccess.createTimestamp.equals(expectedIssuedDate.toDate())
        unmarshalledScopeAccess.clientId.equals(identityConfig.getStaticConfig().getCloudAuthClientId())
        unmarshalledScopeAccess.authenticatedBy.size() == 1
        unmarshalledScopeAccess.authenticatedBy.get(0).equals(AuthenticatedByMethodEnum.PASSWORD.getValue())
        unmarshalledScopeAccess.issuedToUserId.equals(expectedIssuedToUserId)

        //unused fields
        StringUtils.isEmpty(unmarshalledScopeAccess.scope)
        StringUtils.isEmpty(unmarshalledScopeAccess.clientRCN)
        unmarshalledScopeAccess.refreshTokenExp == null
        unmarshalledScopeAccess.userPasswordExpirationDate == null
        StringUtils.isEmpty(unmarshalledScopeAccess.refreshTokenString)
        StringUtils.isEmpty(unmarshalledScopeAccess.clientRCN)
    }

    def "can only unmarshall v3 provisioned user token when feature enabled."() {
        when: "feature enabled"
        reloadableConfig.setProperty(IdentityConfig.FEATURE_SUPPORT_V3_PROVISIONED_USER_TOKENS_PROP, true)

        then: "can unmarshall provisioned user unscoped token"
        aeTokenService.unmarshallToken(provisionedUserUnscopedToken) != null

        and: "can unmarshall provisioned user project scoped token"
        aeTokenService.unmarshallToken(provisionedUserProjectScopedToken) != null

        when: "feature disabled and unmarshall unscoped"
        reloadableConfig.setProperty(IdentityConfig.FEATURE_SUPPORT_V3_PROVISIONED_USER_TOKENS_PROP, false)
        aeTokenService.unmarshallToken(provisionedUserUnscopedToken)

        then: "get UnmarshallTokenException"
        def uex = thrown(UnmarshallTokenException)
        uex.errorCode == DefaultAETokenService.ERROR_CODE_UNMARSHALL_INVALID_ENCRYPTION_SCHEME

        when: "feature disabled and unmarshall project scoped"
        reloadableConfig.setProperty(IdentityConfig.FEATURE_SUPPORT_V3_PROVISIONED_USER_TOKENS_PROP, false)
        aeTokenService.unmarshallToken(provisionedUserProjectScopedToken)

        then: "get UnmarshallTokenException"
        def pex = thrown(UnmarshallTokenException)
        pex.errorCode == DefaultAETokenService.ERROR_CODE_UNMARSHALL_INVALID_ENCRYPTION_SCHEME
    }
}
