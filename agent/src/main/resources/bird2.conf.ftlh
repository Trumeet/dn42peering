<#if !mpbgp>
protocol bgp dn42_${name?long?c} from dnpeers {
        neighbor ${ipv4} as ${asn?long?c};
        direct;
}
</#if>
<#if ipv6??>
protocol bgp dn42_${name?long?c}_v6 from dnpeers {
        neighbor ${ipv6}%${dev} as ${asn?long?c};
        direct;
}
</#if>