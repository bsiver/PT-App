package com.android.priceticker;

import org.bson.types.ObjectId;


public class PriceTick implements Comparable<PriceTick>{

	private Instrument instrument;
	private LatestPrices latestprices;
	private Greeks greeks;
	
	private String lastupdatetime;
	private String volume;
	private ObjectId _id;
	
	public PriceTick(Instrument instrument, LatestPrices latestprices,
			Greeks greeks, String lastupdatetime, String volume, ObjectId _id) {
		super();
		this.instrument = instrument;
		this.latestprices = latestprices;
		this.greeks = greeks;
		this.lastupdatetime = lastupdatetime;
		this.volume = volume;
		this._id = _id;
	}
	
	public PriceTick() {
		super();
	}
	
	public Instrument getInstrument() {
		return instrument;
	}

	public void setInstrument(Instrument instrument) {
		this.instrument = instrument;
	}

	public LatestPrices getLatestprices() {
		return latestprices;
	}

	public void setLatestprices(LatestPrices latestprices) {
		this.latestprices = latestprices;
	}

	public Greeks getGreeks() {
		return greeks;
	}

	public void setGreeks(Greeks greeks) {
		this.greeks = greeks;
	}

	public String getLastupdatetime() {
		return lastupdatetime;
	}

	public void setLastupdatetime(String lastupdatetime) {
		this.lastupdatetime = lastupdatetime;
	}

	public String getVolume() {
		return volume;
	}

	public void setVolume(String volume) {
		this.volume = volume;
	}
	
	public ObjectId get_id() {
		return _id;
	}

	public void set_id(ObjectId _id) {
		this._id = _id;
	}

	public static class Instrument {
		private String exchange;
		private String commodity;
		private String putcall;
		private double strike;
		private String expiredate;
		
		public Instrument(String exchange, String commodity, String putcall,
				double strike, String expiredate) {
			super();
			this.exchange = exchange;
			this.commodity = commodity;
			this.putcall = putcall;
			this.strike = strike;
			this.expiredate = expiredate;
		}
		public String getExchange() {
			return exchange;
		}
		public void setExchange(String exchange) {
			this.exchange = exchange;
		}
		public String getCommodity() {
			return commodity;
		}
		public void setCommodity(String commodity) {
			this.commodity = commodity;
		}
		public String getPutcall() {
			return putcall;
		}
		public void setPutcall(String putcall) {
			this.putcall = putcall;
		}
		public double getStrike() {
			return strike;
		}
		public void setStrike(double strike) {
			this.strike = strike;
		}
		public String getExpiredate() {
			return expiredate;
		}
		public void setExpiredate(String expiredate) {
			this.expiredate = expiredate;
		}
	}
	
	public static class LatestPrices {
		private double bidprice;
		private double bidquantity;
		private double askprice;
		private double askquantity;
		private String lasttradetime;
		private String lasttradequantity;
		private double premium;
		
		public LatestPrices(double bidprice, double bidquantity,
				double askprice, double askquantity, String lasttradetime,
				String lasttradequantity, double premium) {
			super();
			this.bidprice = bidprice;
			this.bidquantity = bidquantity;
			this.askprice = askprice;
			this.askquantity = askquantity;
			this.lasttradetime = lasttradetime;
			this.lasttradequantity = lasttradequantity;
			this.premium = premium;
		}
		public double getBidprice() {
			return bidprice;
		}
		public void setBidprice(double bidprice) {
			this.bidprice = bidprice;
		}
		public double getBidquantity() {
			return bidquantity;
		}
		public void setBidquantity(double bidquantity) {
			this.bidquantity = bidquantity;
		}
		public double getAskprice() {
			return askprice;
		}
		public void setAskprice(double askprice) {
			this.askprice = askprice;
		}
		public double getAskquantity() {
			return askquantity;
		}
		public void setAskquantity(double askquantity) {
			this.askquantity = askquantity;
		}
		public String getLasttradetime() {
			return lasttradetime;
		}
		public void setLasttradetime(String lasttradetime) {
			this.lasttradetime = lasttradetime;
		}
		public String getLasttradequantity() {
			return lasttradequantity;
		}
		public void setLasttradequantity(String lasttradequantity) {
			this.lasttradequantity = lasttradequantity;
		}
		public double getPremium() {
			return premium;
		}
		public void setPremium(double premium) {
			this.premium = premium;
		}
	}
	
	public static class Greeks {
		private double delta;
		private double gamma;
		private double vega;
		private double theta;
		private double rho;
		
		public double getDelta() {
			return delta;
		}
		public void setDelta(double delta) {
			this.delta = delta;
		}
		public double getGamma() {
			return gamma;
		}
		public void setGamma(double gamma) {
			this.gamma = gamma;
		}
		public double getVega() {
			return vega;
		}
		public void setVega(double vega) {
			this.vega = vega;
		}
		public double getTheta() {
			return theta;
		}
		public void setTheta(double theta) {
			this.theta = theta;
		}
		public double getRho() {
			return rho;
		}
		public void setRho(double rho) {
			this.rho = rho;
		}
	}
	@Override
	public int compareTo(PriceTick other) {
		final int BEFORE = -1;
		final int EQUAL = 0;
		final int AFTER = 1;
		
		double myStrike = this.getInstrument().getStrike();
		double theirStrike = other.getInstrument().getStrike();
		
		if (myStrike < theirStrike) return AFTER;
		if (myStrike == theirStrike) return EQUAL;
		return BEFORE;
	}

}
