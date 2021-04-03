package moe.yuuta.dn42peering.agent.ip;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Link{

	@JsonProperty("qdisc")
	private String qdisc;

	@JsonProperty("ifindex")
	private Integer ifindex;

	@JsonProperty("linkmode")
	private String linkmode;

	@JsonProperty("max_mtu")
	private Integer maxMtu;

	@JsonProperty("flags")
	private List<String> flags;

	@JsonProperty("txqlen")
	private Integer txqlen;

	@JsonProperty("num_rx_queues")
	private Integer numRxQueues;

	@JsonProperty("inet6_addr_gen_mode")
	private String inet6AddrGenMode;

	@JsonProperty("min_mtu")
	private Integer minMtu;

	@JsonProperty("mtu")
	private Integer mtu;

	@JsonProperty("link_type")
	private String linkType;

	@JsonProperty("gso_max_segs")
	private Integer gsoMaxSegs;

	@JsonProperty("ifname")
	private String ifname;

	@JsonProperty("num_tx_queues")
	private Integer numTxQueues;

	@JsonProperty("promiscuity")
	private Integer promiscuity;

	@JsonProperty("operstate")
	private String operstate;

	@JsonProperty("gso_max_size")
	private Integer gsoMaxSize;

	@JsonProperty("group")
	private String group;

	@JsonProperty("linkinfo")
	private Linkinfo linkinfo;

	public void setQdisc(String qdisc){
		this.qdisc = qdisc;
	}

	public String getQdisc(){
		return qdisc;
	}

	public void setIfindex(Integer ifindex){
		this.ifindex = ifindex;
	}

	public Integer getIfindex(){
		return ifindex;
	}

	public void setLinkmode(String linkmode){
		this.linkmode = linkmode;
	}

	public String getLinkmode(){
		return linkmode;
	}

	public void setMaxMtu(Integer maxMtu){
		this.maxMtu = maxMtu;
	}

	public Integer getMaxMtu(){
		return maxMtu;
	}

	public void setFlags(List<String> flags){
		this.flags = flags;
	}

	public List<String> getFlags(){
		return flags;
	}

	public void setTxqlen(Integer txqlen){
		this.txqlen = txqlen;
	}

	public Integer getTxqlen(){
		return txqlen;
	}

	public void setNumRxQueues(Integer numRxQueues){
		this.numRxQueues = numRxQueues;
	}

	public Integer getNumRxQueues(){
		return numRxQueues;
	}

	public void setInet6AddrGenMode(String inet6AddrGenMode){
		this.inet6AddrGenMode = inet6AddrGenMode;
	}

	public String getInet6AddrGenMode(){
		return inet6AddrGenMode;
	}

	public void setMinMtu(Integer minMtu){
		this.minMtu = minMtu;
	}

	public Integer getMinMtu(){
		return minMtu;
	}

	public void setMtu(Integer mtu){
		this.mtu = mtu;
	}

	public Integer getMtu(){
		return mtu;
	}

	public void setLinkType(String linkType){
		this.linkType = linkType;
	}

	public String getLinkType(){
		return linkType;
	}

	public void setGsoMaxSegs(Integer gsoMaxSegs){
		this.gsoMaxSegs = gsoMaxSegs;
	}

	public Integer getGsoMaxSegs(){
		return gsoMaxSegs;
	}

	public void setIfname(String ifname){
		this.ifname = ifname;
	}

	public String getIfname(){
		return ifname;
	}

	public void setNumTxQueues(Integer numTxQueues){
		this.numTxQueues = numTxQueues;
	}

	public Integer getNumTxQueues(){
		return numTxQueues;
	}

	public void setPromiscuity(Integer promiscuity){
		this.promiscuity = promiscuity;
	}

	public Integer getPromiscuity(){
		return promiscuity;
	}

	public void setOperstate(String operstate){
		this.operstate = operstate;
	}

	public String getOperstate(){
		return operstate;
	}

	public void setGsoMaxSize(Integer gsoMaxSize){
		this.gsoMaxSize = gsoMaxSize;
	}

	public Integer getGsoMaxSize(){
		return gsoMaxSize;
	}

	public void setGroup(String group){
		this.group = group;
	}

	public String getGroup(){
		return group;
	}

	public void setLinkinfo(Linkinfo linkinfo){
		this.linkinfo = linkinfo;
	}

	public Linkinfo getLinkinfo(){
		return linkinfo;
	}
}