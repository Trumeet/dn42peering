protocol bgp ${name} from dnpeers {
        neighbor ${ipv4} as ${asn};
        direct;
}

