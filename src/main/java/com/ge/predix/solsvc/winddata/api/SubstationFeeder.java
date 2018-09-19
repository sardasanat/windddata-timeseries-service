package com.ge.predix.solsvc.winddata.api;

public class SubstationFeeder {

	private long timestamp;
	private int load;
	private int quality;
	
	public long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	public int getLoad() {
		return load;
	}
	public void setLoad(int load) {
		this.load = load;
	}	
	public int getQuality() {
		return quality;
	}
	public void setQuality(int quality) {
		this.quality = quality;
	}
	
	@Override
	public String toString(){
		return "\nTimestamp="+getTimestamp()+"::Load="+getLoad()+" watts"+"::DataQuality="+getQuality();
	}



}
