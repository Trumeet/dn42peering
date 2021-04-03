package moe.yuuta.dn42peering.agent.ip;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Linkinfo{

	@JsonProperty("info_kind")
	private String infoKind;

	public void setInfoKind(String infoKind){
		this.infoKind = infoKind;
	}

	public String getInfoKind(){
		return infoKind;
	}
}