package at.ofai.music.match;

class Path {

	public static final int MAX_RUN_LENGTH = 50;
	protected static int[] val = new int[10000];
	protected static int[] len = new int[10000];

	/** Smooths an alignment path.<BR>
	 *  Consider the path as a sequence of horizontal (H), vertical (V) and
	 *  diagonal (D) steps.  The smoothing consists of 2 rewrite rules:<BR>
	 *  HnDmVn / Dm+n (where m is less than MAX_RUN_LENGTH)<BR>
	 *  VnDmHn / Dm+n (where m is less than MAX_RUN_LENGTH)<BR>
	 *  The new path is written over the old path.  Note that the end points of
	 *  each application of a rewrite rule do not change.
	 *  @return the length of the new path
	 */
	public static int smooth(int[] x, int[] y, int length) {
		if (length == 0)
			return 0;
		if (val.length < length) {
			val = new int[length];
			len = new int[length];
		}
		int p = 0;
		val[0] = len[0] = 0;
		for (int i = 1; i < length; i++) {	// H = 1; V = 2; D = 3
			int current = x[i] - x[i-1] + 2 * (y[i] - y[i-1]);
			if (current == val[p]) {
				len[p]++;
			} else if ((current == 3) || (val[p] == 0)) {
				val[++p] = current;
				len[p] = 1;
			} else if (val[p] + current == 3) {	// 1 + 2
				if (--len[p] == 0)
					p--;
				if (val[p] == 3)
					len[p]++;
				else {
					val[++p] = 3;
					len[p] = 1;
				}
			} else {	// val[p] == 3 && current != 3
				if ((val[p-1] == current) ||
						(val[p-1] == 0) ||
						(len[p] > MAX_RUN_LENGTH)) {
					val[++p] = current;
					len[p] = 1;
				} else {
					if (--len[p-1] == 0) {
						val[p-1] = val[p];
						len[p-1] = len[p];
						p--;
						if (val[p-1] == 3) {
							len[p-1] += len[p];
							p--;
						}
					}
					len[p]++;
				}
			}
		}
		int i = 1;
		for (int pp = 1; pp <= p; pp++) {
			int dx = val[pp] & 1;
			int dy = val[pp] >> 1;
			for (int j = len[pp]; j > 0; j--, i++) {
				x[i] = x[i-1] + dx;
				y[i] = y[i-1] + dy;
			}
		}
		return i;
	} // smooth()

} // class Path
