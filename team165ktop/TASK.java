package team165;

public enum TASK {
//	ARCHON_RANDOM(0),
//	ARCHON_STAY_SPAWN(2),
//	ARCHON_RANDOM_SPAWN(5),
//	ARCHON_BACKUP(6),
//	ARCHON_CAPTURE_POWERNODE(7),
//	ARCHON_MOVE(8),
//	ARCHON_FLEE(9),
//	ARCHON_MOVE_SPAWN(10),
	
	HQSPAWN(100),
	HQATTACK(200),
	HQATTACKPASTR(300),
	HQMonitorPASTRConstruc(1000),
//	FOLLOW(100),	
	STAY(400), // literally means defend
	ASSEMBLE(500),
	BUILDPASTR(600),
	ATTACK(700),
	RANDOM(800),
	
	NTherdCow(900),
	NTSelfDestruct(1700),
	
	NONE(-999);

	public final int task;
		
	TASK(int task) {
		this.task = task;
	}
	
    public int getTask(){return task;}
    public boolean isEmpty(){return this.equals(TASK.NONE);}
    public boolean compare(int t){return task == t;}
    public static TASK getTask(int _t)
    {
        TASK[] taskVec = TASK.values();
        //note the < instead of != here
        for(int i = 0; i < taskVec.length; i++)
        {
            if(taskVec[i].compare(_t))
                return taskVec[i];
        }
        return TASK.NONE;
    }
	
	
}
