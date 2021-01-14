protocol bgp dn42_${name} from dnpeers {
        neighbor ${ipv4} as ${asn};
        direct;
}

