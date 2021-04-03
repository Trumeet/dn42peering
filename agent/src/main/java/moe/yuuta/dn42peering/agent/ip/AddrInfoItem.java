package moe.yuuta.dn42peering.agent.ip;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AddrInfoItem{

	@JsonProperty("address")
	private String address;

	@JsonProperty("scope")
	private String scope;

	@JsonProperty("prefixlen")
	private Integer prefixlen;

	@JsonProperty("valid_life_time")
	private Long validLifeTime;

	@JsonProperty("family")
	private String family;

	@JsonProperty("preferred_life_time")
	private Long preferredLifeTime;

	@JsonProperty("local")
	private String local;

	@JsonProperty("label")
	private String label;

	public void setAddress(String address){
		this.address = address;
	}

	public String getAddress(){
		return address;
	}

	public void setScope(String scope){
		this.scope = scope;
	}

	public String getScope(){
		return scope;
	}

	public void setPrefixlen(Integer prefixlen){
		this.prefixlen = prefixlen;
	}

	public Integer getPrefixlen(){
		return prefixlen;
	}

	public void setValidLifeTime(Long validLifeTime){
		this.validLifeTime = validLifeTime;
	}

	public Long getValidLifeTime(){
		return validLifeTime;
	}

	public void setFamily(String family){
		this.family = family;
	}

	public String getFamily(){
		return family;
	}

	public void setPreferredLifeTime(Long preferredLifeTime){
		this.preferredLifeTime = preferredLifeTime;
	}

	public Long getPreferredLifeTime(){
		return preferredLifeTime;
	}

	public void setLocal(String local){
		this.local = local;
	}

	public String getLocal(){
		return local;
	}

	public void setLabel(String label){
		this.label = label;
	}

	public String getLabel(){
		return label;
	}
}