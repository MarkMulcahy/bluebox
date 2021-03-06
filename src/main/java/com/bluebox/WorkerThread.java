package com.bluebox;

import java.time.LocalTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WorkerThread implements Runnable {

	private int progress = 0;
	private String id, status;
	private boolean stop = false;
	private static Map<String,WorkerThread> workers = new HashMap<String,WorkerThread>();
	private static Map<String,Thread> workerThreads = new HashMap<String,Thread>();
	private static final Logger log = LoggerFactory.getLogger(WorkerThread.class);
	private Date started = new Date(), ended;

	public WorkerThread(String id) throws Exception {
		if (workers.containsKey(id)) {
			if (workers.get(id).getProgress()<100)
				throw new Exception("Task already started ("+workers.get(id).getProgress()+"%)");
			else {
				log.debug("Removing stale task");
				workers.remove(id);
			}
		}
		this.id = id;
		this.status = "Initialising";
		log.error("Initialising thread {}",id);
		workers.put(id, this);
	}

	public String getId() {
		return id;
	}

	public int getProgress() {
		return progress;
	}

	public void setProgress(int p) {
		this.progress = p;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status+" ("+getElapsedTime()+")";
	}

	@Override
	public boolean equals(Object obj) {
		if (((WorkerThread)obj).getId().equals(getId()))
			return true;
		return super.equals(obj);
	}

	public abstract void run();

	/*
	 * Used to allow external threads to invoke arbitrary code on this running thread
	 */
	public void generic(Object obj) {

	}

	public void stop() {
		stop  = true;
	}

	public boolean isStopped() {
		return stop;
	}

	public static void stopWorkers() {
		for (WorkerThread tw : workers.values()) {
			tw.stop();
		}		
	}

	public static void stopWorker(String id) {
		WorkerThread tw = workers.get(id);
		if (tw!=null) {
			tw.setProgress(100);
			tw.stop();
		}
		else {
			log.error("Problem stopping unknown thread {}",id);
			new Exception("Steve").printStackTrace();
		}
	}

	public static void startWorker(String id) {
		WorkerThread tw = workers.get(id);
		if (tw!=null) {
			startWorker(tw);
		}
		else {
			log.error("Problem starting unknown thread []",id);
		}
	}

	public static Thread startWorker(WorkerThread tw) {
		Thread t = new Thread(tw);
		t.start();
		workerThreads.put(tw.getId(), t);
		return t;
	}

	public static JSONObject getWorkerStatus() throws JSONException {
		JSONObject jo = new JSONObject();
		for (WorkerThread tw : workers.values()) {
			if (tw.getProgress()<=100) {
				jo.put(tw.getId(), tw.getProgress());
				jo.put(tw.getId()+"_status", tw.getStatus());
			}
		}
		return jo;
	}

	/*
	 * Get the running instance of a worker.
	 */
	public static WorkerThread getInstance(String id) {
		if (workers.containsKey(id)) {
			if (workers.get(id).getProgress()<100)
				return workers.get(id);
		}
		return null;
	}

	public String getElapsedTime(Date now) {
		String s = LocalTime.MIN.plusSeconds((now.getTime()-started.getTime())/1000).toString();
		return s;
	}

	public String getElapsedTime() {
		if (started==null)
			started = new Date();
		if (isStopped()) {
			return getElapsedTime(ended);
		}
		else {
			ended = new Date();
			return getElapsedTime(ended);
		}
	}
}
