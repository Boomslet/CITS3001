
public class MancalaImp implements MancalaAgent {
	public static final int DEPTH = 10;

	@Override
	public int move(int[] board) {
		return findBestMove(board);
	}
	
	public int findBestMove(int[] boardState) {
		int bestMove = 0;
		double bestScore = Integer.MIN_VALUE;
		
		for (int i = 0; i < 6; i++) {
			if (boardState[i] == 0) continue;
			
			int[] board = boardState.clone();
			
			boolean extraMove = makePlayerMove(i, board);
			
			double moveScore = minimax(board, DEPTH, extraMove, Integer.MIN_VALUE, Integer.MAX_VALUE);
			
			if (moveScore > bestScore) {
				bestScore = moveScore; 
				bestMove = i;
			}
		}
		
		return bestMove;
	}
	
	public double evaluateBoard(int[] board) {
		double sum = board[6] - board[13] * 0.75;
		
		double playerScale = 2.5;
		double opponentScale = 2.5;
		
		for (int i = 0; i < 6; i++) {
			// slightly favour left-most seeds
			sum += board[i] * playerScale;
			// heavily punish overflow
			if (board[i] > 6 - i) {
				sum -= 10 * board[i] - (6 - i);
			}
			playerScale -= 0.35;
		}
		
		for (int j = 7; j < 13; j++) {
			// slightly favour left-most seeds
			sum -= board[j] * opponentScale;
			// heavily punish overflow
			if (board[j] > 13 - j) {
				sum += 10 * board[j] - (13 - j);
			}
			opponentScale -= 0.35;
		}
		
		return sum;
	}
	
	public double minimax(int[] boardState, int depth, boolean max, double alpha, double beta) {
		int[] board;
		
		if (gameOver(boardState)) {
			if (boardState[6] > boardState[13]) return Integer.MAX_VALUE - 1;
			if (boardState[13] > boardState[3]) return Integer.MIN_VALUE + 1;
			return 0;
		}
		
		if (depth == 0) {
			return evaluateBoard(boardState);
		}
		
		if (max) {
			double score = Integer.MIN_VALUE;
			
			for (int i = 0; i < 6; i++) {
				if (boardState[i] == 0) continue;
				
				board = boardState.clone();
				
				boolean extraMove = makePlayerMove(i, board);

				double moveValue = minimax(board, depth - 1, extraMove, alpha, beta);
				
				score = Math.max(score, moveValue);
				alpha = Math.max(alpha, score);
				
				if (beta <= alpha) break;
			}
			
			return score;
		}
		else {
			double score = Integer.MAX_VALUE;
			
			for (int j = 7; j < 13; j++) {
				if (boardState[j] == 0) continue;
				
				board = boardState.clone();
				
				boolean extraMove = makeOpponentMove(j, board);
				
				double moveValue = minimax(board, depth - 1, !extraMove, alpha, beta);
				
				score = Math.min(score, moveValue);
				beta = Math.min(beta, score);
				
				if (beta <= alpha) break;
			}
			return score;
		}		
	}
	
	/**
	 * Code pinched from Mancala.java
	 */
	public boolean makePlayerMove(int index, int[] board) {
		int i = index;
		
		while (board[index] > 0) {
			i = i == 12 ? 0 : i + 1;
			board[i]++; board[index]--;
		}
	       
		if(i < 6 && board[i] == 1 && board[12 - i] > 0) {
	         board[6] += board[12 - i]; 
	         board[12 - i] = 0;
	         board[6] += board[i];
	         board[i] = 0;
	       }
		
		if (i != 6) return false;
		return true;
	}
	
	/**
	 * Code pinched from Mancala.java
	 */
	public boolean makeOpponentMove(int index, int[] board) {
		int i = index;
		
		while (board[index] > 0) {
			i = i == 5 ? 7 : i == 13 ? 0 : i + 1;
			board[i]++; board[index]--;
		}
	       
		if(i < 13 && i > 6 && board[i] == 1 && board[12 - i] > 0) {
	         board[13] += board[12 - i]; 
	         board[12 - i] = 0;
	         board[13] += board[i];
	         board[i] = 0;
	       }
		
		if (i != 13) return false;
		return true;
	}
	
	public boolean gameOver(int[] board) {
		boolean noPlayerMoves = true;
		boolean noAgentMoves = true;
		
		for (int i = 0; i < 6; i++) {
			if (board[i] != 0) {
				noPlayerMoves = false;
				break;
			}
		}
		
		for (int j = 7; j < 13; j++) {
			if (board[j] != 0) {
				noAgentMoves = false;
				break;
			}
		}
		
		if (noPlayerMoves) {
			for (int j = 7; j < 13; j++) {
				board[13] += board[j];
			}
		}
		
		if (noAgentMoves) {
			for (int i = 0; i < 6; i++) {
				board[6] += board[i];
			}
		}
		
		return (noPlayerMoves || noAgentMoves);
	}

	@Override
	public String name() {
		return "The MANcala Agent";
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}
}
