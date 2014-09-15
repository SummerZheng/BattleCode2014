package team165;

public enum STATE {

	MOVE(101), STAY(202);

	public final int state;

	STATE(int state) {
		this.state = state;
	}
}
