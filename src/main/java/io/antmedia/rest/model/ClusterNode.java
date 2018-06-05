package io.antmedia.rest.model;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Indexes;

@Entity("clusternode")

@Indexes({ @Index(fields = @Field("id"))})

public class ClusterNode {
	
	@Id
	private String id;
	private String ip;
	private String status;
	
	public ClusterNode() {
		// TODO Auto-generated constructor stub
	}
	
	public ClusterNode(String ip) {
		super();
		this.ip = ip;
		this.id = ip.replace(".", "_");
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
