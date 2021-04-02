SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;

CREATE TABLE `asn` (
  `asn` char(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `activated` bit(1) NOT NULL DEFAULT b'0',
  `register_date` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `node` (
  `id` int(10) UNSIGNED NOT NULL,
  `public_ip` varchar(21) COLLATE utf8mb4_unicode_ci NOT NULL,
  `dn42_ip4` varchar(21) COLLATE utf8mb4_unicode_ci NOT NULL,
  `dn42_ip6` varchar(39) COLLATE utf8mb4_unicode_ci NOT NULL,
  `asn` char(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `internal_ip` varchar(15) COLLATE utf8mb4_unicode_ci NOT NULL,
  `internal_port` smallint(5) UNSIGNED NOT NULL,
  `name` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `notice` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `vpn_type_wg` tinyint(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `peer` (
  `id` int(11) NOT NULL,
  `type` enum('WIREGUARD') COLLATE utf8mb4_unicode_ci NOT NULL,
  `asn` char(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ipv4` varchar(15) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ipv6` varchar(39) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wg_endpoint` varchar(15) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wg_endpoint_port` smallint(5) UNSIGNED DEFAULT NULL,
  `wg_self_pubkey` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wg_self_privkey` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wg_peer_pubkey` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wg_preshared_secret` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `provision_status` enum('NOT_PROVISIONED','PROVISIONED','FAIL') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NOT_PROVISIONED',
  `mpbgp` tinyint(1) NOT NULL,
  `node` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


ALTER TABLE `asn`
  ADD PRIMARY KEY (`asn`);

ALTER TABLE `node`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `peer`
  ADD PRIMARY KEY (`id`);


ALTER TABLE `node`
  MODIFY `id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT;

ALTER TABLE `peer`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;
COMMIT;
