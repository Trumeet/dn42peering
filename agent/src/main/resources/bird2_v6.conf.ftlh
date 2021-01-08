protocol bgp ${name}_v6 from dnpeers {
        neighbor ${ipv6}%${dev} as ${asn};
        direct;
}