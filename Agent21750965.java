package agents;

import hanabAI.Action;
import hanabAI.ActionType;
import hanabAI.Agent;
import hanabAI.Card;
import hanabAI.Colour;
import hanabAI.IllegalActionException;
import hanabAI.State;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Stack;
import java.util.Map.Entry;

public class Agent21750965 implements Agent {
	// List of the remaining unseen Cards
	public ArrayList<Card> remainingDeck;

	// Represents the size of the discards pile
	public int discardsSize;
	// Represents the size of each fireworks stack
	public int red;
	public int yellow;
	public int green;
	public int blue;
	public int white;

	// Each players card age (n turns held)
	public int[][] ages;
	// Each players Colour hints
	public Colour[][] colours;
	// Each players value hints
	public int[][] values;

	// This Agent's index
	private int playerIndex;
	// Number of Agents in the game
	private int playerCount;
	// Number of Cards in a hand
	private int handSize;
	// Represents the first turn of the game
	private boolean firstAction = true;
	// GameState containing current game information
	private GameState gameState;

	/**
	 * Default constructor.
	 */
	public Agent21750965() { }

	/**
	 * Original author - Tim French.
	 * 
	 * Initialises Agent variables on first doAction call.
	 * 
	 * @param s State of the current game
	 */
	public void init(State s) {
		playerCount = s.getPlayers().length;
		remainingDeck = new ArrayList<Card>(Arrays.asList(Card.getDeck()));

		discardsSize = s.getDiscards().size();
		red = s.getFirework(Colour.RED).size();
		yellow = s.getFirework(Colour.YELLOW).size();
		green = s.getFirework(Colour.GREEN).size();
		blue = s.getFirework(Colour.BLUE).size();
		white = s.getFirework(Colour.WHITE).size();

		// Remove discards from remainingDeck
		for (Card discard : s.getDiscards()) {
			remainingDeck.remove(discard);
		}

		if (playerCount > 3) {
			handSize = 4;
		} else {
			handSize = 5;
		}

		ages = new int[playerCount][handSize];
		colours = new Colour[playerCount][handSize];
		values = new int[playerCount][handSize];

		gameState = new GameState(playerCount);

		updateGameState(s);

		playerIndex = s.getNextPlayer();
		firstAction = false;
	}

	/**
	 * Updates the Agent's GameState to reflect the State object.
	 * 
	 * @param s State of the current game
	 */
	@SuppressWarnings("unchecked")
	public void updateGameState(State s) {
		gameState.players = s.getPlayers();
		gameState.discards = s.getDiscards();
		gameState.fuse = s.getFuseTokens();
		gameState.order = s.getOrder();
		gameState.hints = s.getHintTokens();
		gameState.nextPlayer = s.getNextPlayer();
		gameState.finalAction = s.getFinalActionIndex();

		for (int i = 0; i < playerCount; i++) {
			gameState.colours[i] = (Colour[]) colours[i].clone();
			gameState.values[i] = (int[]) values[i].clone();
			gameState.ages[i] = (int[]) ages[i].clone();
		}

		for (Colour c : Colour.values()) {
			gameState.fireworks.put(c, (Stack<Card>) s.getFirework(c).clone());
		}

		for (int i = 0; i < playerCount; i++) {
			if (i == playerIndex) continue;
			gameState.hands[i] = s.getHand(i);
		}
	}

	/**
	 * Original author - Tim French.
	 * 
	 * Updates the colours and values arrays with any
	 * Action information in the previous state actions.
	 * 
	 * @param s the State object to inspect
	 */
	public void getHints(State s) {
		try {
			State t = (State) s.clone();
			Action a;
			ActionType type;

			for (int i = 0; i < Math.min(playerCount - 1, s.getOrder()); i++) {
				a = t.getPreviousAction();
				type = a.getType();

				if (type == ActionType.HINT_COLOUR || type == ActionType.HINT_VALUE) {
					boolean[] hints = a.getHintedCards();

					for (int j = 0; j < handSize; j++) {
						if (hints[j]) {
							if (type == ActionType.HINT_COLOUR)
								colours[a.getHintReceiver()][j] = a.getColour();
							else
								values[a.getHintReceiver()][j] = a.getValue();
						}
					}
				}

				if (type == ActionType.DISCARD || type == ActionType.PLAY) {
					ages[a.getPlayer()][a.getCard()] = 0;
					colours[a.getPlayer()][a.getCard()] = null;
					values[a.getPlayer()][a.getCard()] = 0;
				}

				t = t.getPreviousState();
			}
		} 
		catch (IllegalActionException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Original author - Tim French.
	 * 
	 * Follows a sequential procedure of updating local
	 * information before determining and returning
	 * the best Action on the given state s
	 * 
	 * @param s the current State 
	 * @return the best Action on the given State
	 */
	public Action doAction(State s) {
		if (firstAction) {
			init(s);
		}

		getHints(s);
		updateHandAges();
		updateDeckList(s);
		Stack<Card> generatedDeck = generateDeck(s);
		updateGameState(s);
		GameState gsClone = (GameState) gameState.clone();

		try {
			return findBestMove(gsClone, generatedDeck);
		} 
		catch (IllegalActionException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	/**
	 * Generates and returns a Stack containing all unseen Cards
	 * for the given State s.
	 * 
	 * @param s the current State
	 * @return a Stack of all unseen Cards in the given State
	 */
	@SuppressWarnings("unchecked")
	private Stack<Card> generateDeck(State s) {
		ArrayList<Card> deckListClone = (ArrayList<Card>) remainingDeck.clone();
		
		for (int i = 1; i < playerCount; i++) {
			int currentPlayer = (playerIndex + i) % playerCount;
			
			for (Card c : s.getHand(currentPlayer)) {
				deckListClone.remove(c);
			}
		}
		
		Stack<Card> generatedDeck = new Stack<Card>();
		generatedDeck.addAll(deckListClone);
		return generatedDeck;
	}

	/**
	 * Updates the deckList ArrayList containing all
	 * unaccounted for cards (excluding those held by opponents)
	 * 
	 * @param s the current State
	 */
	private void updateDeckList(State s) {
		Stack<Card> currentPile;
		int currentPileSize;

		// If the size of discards has changed this rotation
		if (discardsSize != s.getDiscards().size()) {
			currentPile = s.getDiscards();
			currentPileSize = currentPile.size();
			int discardsAdded = currentPileSize - discardsSize;

			for (int j = 0; j < discardsAdded; j++) {
				remainingDeck.remove(currentPile.get(currentPileSize - 1 - j));
			}

			discardsSize = s.getDiscards().size();
		}

		// If the size of red fireworks has changed this rotation
		if (red != s.getFirework(Colour.RED).size()) {
			currentPile = s.getFirework(Colour.RED);
			currentPileSize = currentPile.size();
			int redAdded = currentPileSize - red;

			for (int i = 0; i < redAdded; i++) {
				remainingDeck.remove(currentPile.get(currentPileSize - 1 - i));
			}
			
			red = currentPileSize;
		}

		// If the size of yellow fireworks has changed this rotation
		if (yellow != s.getFirework(Colour.YELLOW).size()) {
			currentPile = s.getFirework(Colour.YELLOW);
			currentPileSize = currentPile.size();
			int yellowAdded = currentPileSize - yellow;

			for (int i = 0; i < yellowAdded; i++) {
				remainingDeck.remove(currentPile.get(currentPileSize - 1 - i));
			}
			
			yellow = currentPileSize;
		}

		// If the size of green fireworks has changed this rotation
		if (green != s.getFirework(Colour.GREEN).size()) {
			currentPile = s.getFirework(Colour.GREEN);
			currentPileSize = currentPile.size();
			int greenAdded = currentPileSize - green;

			for (int i = 0; i < greenAdded; i++) {
				remainingDeck.remove(currentPile.get(currentPileSize - 1 - i));
			}

			green = currentPileSize;
		}

		// If the size of blue fireworks has changed this rotation
		if (blue != s.getFirework(Colour.BLUE).size()) {
			currentPile = s.getFirework(Colour.BLUE);
			currentPileSize = currentPile.size();
			int blueAdded = currentPileSize - blue;

			for (int i = 0; i < blueAdded; i++) {
				remainingDeck.remove(currentPile.get(currentPileSize - 1 - i));
			}

			blue = currentPileSize;
		}

		// If the size of white fireworks has changed this rotation
		if (white != s.getFirework(Colour.WHITE).size()) {
			currentPile = s.getFirework(Colour.WHITE);
			currentPileSize = currentPile.size();
			int whiteAdded = currentPileSize - white;

			for (int i = 0; i < whiteAdded; i++) {
				remainingDeck.remove(currentPile.get(currentPileSize - 1 - i));
			}

			white = currentPileSize;
		}
	}

	/**
	 * Updates the ages of all currently 
	 * held cards in all hands.
	 */
	private void updateHandAges() {
		for (int i = 0; i < playerCount; i++) {
			for (int j = 0; j < handSize; j++) {
				ages[i][j]++;
			}
		}

	}

	/**
	 * Begins the IS-UCT procedure and returns the
	 * best Action calculated from the algorithm. 
	 * The determined Action will be used to update 
	 * the colours and values arrays.
	 *   
	 * @param gameState the current GameState
	 * @param deck the deck of unseen Cards
	 * @return the best Action for the given State
	 * @throws IllegalActionException if an illegal Action is applied to the GameState
	 */
	public Action findBestMove(GameState gameState, Stack<Card> deck) throws IllegalActionException {
		MCTStree mctsTree = new MCTStree(gameState, deck);
		Node bestNode = mctsTree.ISUCT();

		// Use the information from our action to update our hint arrays
		if (bestNode.action.getType() == ActionType.DISCARD || bestNode.action.getType() == ActionType.PLAY) {
			ages[playerIndex][bestNode.action.getCard()] = 0;
			colours[playerIndex][bestNode.action.getCard()] = null;
			values[playerIndex][bestNode.action.getCard()] = 0;
		}

		// Use the information from our action to update our hint arrays
		if (bestNode.action.getType() == ActionType.HINT_COLOUR || bestNode.action.getType() == ActionType.HINT_VALUE) {
			boolean[] hints = bestNode.action.getHintedCards();

			for (int j = 0; j < hints.length; j++) {
				if (hints[j]) {
					if (bestNode.action.getType() == ActionType.HINT_COLOUR)
						colours[bestNode.action.getHintReceiver()][j] = bestNode.action.getColour();
					else
						values[bestNode.action.getHintReceiver()][j] = bestNode.action.getValue();
				}
			}
		}
		
		return bestNode.action;
	}

	/**
	 * Returns the Agent's name.
	 * 
	 * @return Agent21750965 - the Agent's name
	 */
	public String toString() {
		return "Agent21750965";
	}
	
	class MCTStree {
		
		// Exploration constant
	    private final double CONST = Math.sqrt(2);
	    // Computational time budget
	    private final int TIME = 800;
	    // Generated deck from the root Node's GameState
	    public Stack<Card> deck;
	    // Root Node of the tree
	    public Node root;
	    // Root state of the tree
	    public GameState gameState;
	    
	    public Random rand = new Random();
		
	    /**
	     * Default Constructor.
	     * 
	     * @param gs the root GameState of the tree
	     * @param deck a deck of Cards that have not been accounted for
	     */
		@SuppressWarnings("unchecked")
		public MCTStree(GameState gs, Stack<Card> deck) {
			this.deck = (Stack<Card>) deck.clone();
			gameState = (GameState) gs.clone();
			root = new Node(null, null);
			root.populatePossibleMoves(gameState);
		}
		
		/**
		 * Performs the IS-MCTS+UCB algorithm on the root GameState, with
		 * an Information Set determined from deck.
		 * 
		 * @return the child Node of root containing the optimal Action
		 * @throws IllegalActionException if an illegal Action is applied to the GameState
		 */
		@SuppressWarnings("unchecked")
		public Node ISUCT() throws IllegalActionException {
			
			// Computational time budget
			long finishTime = System.currentTimeMillis() + TIME;

			Node bestChild = null;
			double bestScore = Integer.MIN_VALUE;
			
			// While within computational budget
			while (System.currentTimeMillis() < finishTime) {
				// Randomly choose a determination
				GameState d = (GameState) gameState.clone();
				d.deck = (Stack<Card>) deck.clone();
				d.determinePlayerHand();
				Collections.shuffle(d.deck);
				
				// Tree policy
				Result<Node, GameState> nd = select(root, d);
				int moveScore = rollout(nd.d);
				backup(nd.n, moveScore);

			}
			
			// find highest scoring child (Action) from root
			for (Node child : root.children) {
				if (child.getUCB(0) > bestScore) {
					bestScore = child.getUCB(0);
					bestChild = child;
				}
			}

			return bestChild;
		}

		/**
		 * The back-propagation phase of IS-UCT. Updates the score 
		 * and number of visits for each Node in the traversal.
		 * 
		 * @param currentNode the final Node in the tree traversal
		 * @param moveScore the score of the current tree traversal
		 */
		private void backup(Node currentNode, int moveScore) {
			while (currentNode != null) {
				currentNode.score += moveScore;
				currentNode.visits++;
				currentNode = currentNode.parent;
			}
		}

		/**
		 * The simulation phase of IS-MCTS+UCB. Determines an Action
		 * to play in the current state according to a sequential
		 * decision tree.
		 * 
		 * @param d the GameState for simulation
		 * @return the integer score of the end state of the simulation
		 * @throws IllegalActionException if an illegal Action is applied to the GameState
		 */
		private int rollout(GameState d) throws IllegalActionException {
			GameState gameStateClone = (GameState) d.clone();
			
			// Code pinched from Tim French
			while (!gameStateClone.gameOver()) {
				Action a = gameStateClone.playKnown();
				if (a == null) a = gameStateClone.discardKnown();
				if (a == null) a = gameStateClone.hintPlayable();
				if (a == null) a = gameStateClone.hintMostCommon();
				if (a == null) a = gameStateClone.discardOldest();
				gameStateClone = gameStateClone.nextState(a);
			}
			
			return gameStateClone.getScore();
		}

		/**
		 * The expansion stage of IS-MCTS+UCB. Adds a child Node to 
		 * Node n that contains a legal move given a GameState d.
		 * 
		 * @param n the parent node to expand
		 * @param d the current GameState
		 * @return
		 * @throws IllegalActionException if an illegal Action is applied to the GameState
		 */
		private Result<Node, GameState> expand(Node n, GameState d) throws IllegalActionException {
			ArrayList<Action> legalMoves = new ArrayList<Action>();
			
			for (Action a: n.possibleMoves) {
				if (d.legalAction(a)) legalMoves.add(a);
			}
			
			// Choose an unplayed move from this state uniformly at random
			// and apply it to the current GameState
			Action action = legalMoves.get(rand.nextInt(legalMoves.size()));
			n.possibleMoves.remove(action);
			d = d.nextState(action);
			
			// Created and add new child n' to n
			Node c = new Node(n, action);
			c.populatePossibleMoves(d);
			n.children.add(c);
			
			return new Result<Node, GameState>(c, d);
		}

		/**
		 * Traverses the tree.
		 * 
		 * @param currentNode the root Node of the tree
		 * @return the Node and GameState pair of the current traversal
		 * @throws IllegalActionException if an illegal Action is applied to the GameState
		 */
		private Result<Node, GameState> select(Node n, GameState d) throws IllegalActionException {
			while (!d.gameOver()) {
				if (n.hasMoves(d)) {
					return expand(n, d);
				}
				else {
					for (Node c : n.children) {
						if (d.legalAction(c.action)) c.availability++;
					}
					n = bestChild(n, d);
					d = d.nextState(n.action);
				}
			}
			
			return new Result<Node, GameState>(n, d);
		}

		/**
		 * Calculates and returns the child Node 
		 * of n with the highest UCB.
		 * 
		 * @param n the Node whose children to inspect
		 * @param d the current GameState
		 * @return the child Node with the highest UCB
		 * @throws IllegalActionException if an illegal Action is applied to the GameState
		 */
		private Node bestChild(Node n, GameState d) throws IllegalActionException {
			Node bestChild = null;
			double highestUCB = -10000;
			double currentUCB = -10000;
			
			for (Node child : n.children) {
				// Skip children with actions that can't 
				// be played on the current state 
				if (!d.legalAction(child.action)) continue;
				currentUCB = child.getUCB(CONST);
				
				if (currentUCB > highestUCB) {
					highestUCB = currentUCB;
					bestChild = child;
				}
			}
			
			return bestChild;
		}
		
		public String toString() { return "MCTS"; }
	}
	
	/**
	 * Represents a Node in the MCTS tree.
	 * 
	 * @author Mark Boon
	 */
	class Node {

		// Parent node of this gameState
		public Node parent;
		// List of Nodes representing tried moves
		public ArrayList<Node> children;
		// List of all untried moves
		public ArrayList<Action> possibleMoves;
		// The action resulting in this gameState
		public Action action;
		// Total MCTS score for this Node
		public double score = 0;
		// Total number of Node visits
		public int visits = 0;
		// Total availability of children
		public int availability = 1;

		/**
		 * Constructs the Node with a parent Node, and the Action 
		 * that resulted in this Node's creation. 
		 * 
		 * @param parent parent Node of this Node (null if root).
		 * @param action the Action corresponding to the GameState.
		 */
		public Node(Node parent, Action action) {
			this.action = action;
			this.parent = parent;

			children = new ArrayList<Node>();
			possibleMoves = new ArrayList<Action>();		
		}

		/**
		 * Populates the possibleMove List with candidate 
		 * moves playable given the current GameState:
		 * 
		 * - Play playable Cards
		 * - Discard useless Cards
		 * - Hint playable Cards
		 * - Hint most revealing Cards 
		 * 
		 * @param gameState the current GameState
		 */
		public void populatePossibleMoves(GameState gameState) {
			int playerIndex = gameState.nextPlayer;
			int handSize = gameState.handSize;
			
			try {
				for (int i = 0; i < handSize; i++) {
					possibleMoves.add(new Action(playerIndex, "Agent21750965", ActionType.PLAY, i));
					
					if (gameState.hints != 8) {
						possibleMoves.add(new Action(playerIndex, "Agent21750965", ActionType.DISCARD, i));
					}

				}
				
				// Adds hint Actions for playable cards
				if (gameState.hints > 0) {
					for (int i = 1; i < gameState.playerCount; i++) {
						int hintee = (playerIndex + i) % gameState.playerCount;
						Card[] hinteeHand = gameState.hands[hintee];

						for (int j = 0; j < handSize; j++) {
							Card c = hinteeHand[j];
							
							if (c != null && c.getValue() == gameState.playable(c.getColour())) {
								if (gameState.values[hintee][j] == 0) {
									boolean[] val = new boolean[hinteeHand.length];

									for (int k = 0; k < val.length; k++) {
										if (hinteeHand[k] == null) continue;
										val[k] = c.getValue() == hinteeHand[k].getValue();
									}
									
									possibleMoves.add(new Action(playerIndex, "Agent21750965", ActionType.HINT_VALUE, hintee, val, c.getValue()));
								} 
								else if (gameState.colours[hintee][j] == null) {
									boolean[] col = new boolean[hinteeHand.length];

									for (int k = 0; k < col.length; k++) {
										if (hinteeHand[k] == null) continue;
										col[k] = c.getColour().equals(hinteeHand[k].getColour());
									}
									
									possibleMoves.add(new Action(playerIndex, "Agent21750965", ActionType.HINT_COLOUR, hintee, col, c.getColour()));
								}
							}
						}
					}

					int mostCommon = -1;
					int bestHintee = (playerIndex + 1) % gameState.playerCount;
					boolean colour = true;
					Colour cKey = null;
					int vKey = -1;

					for (int i = 1; i < gameState.playerCount; i++) {
						int hintee = (gameState.nextPlayer + i) % gameState.playerCount;
						Card[] currentHand = gameState.hands[hintee];
						
						// Stores the number of cards sharing Colour
						Map<Colour, Integer> col = new HashMap<Colour, Integer>();
						// Stores the number of unhinted cards sharing value
						Map<Integer, Integer> val = new HashMap<Integer, Integer>();

						for (int j = 0; j < handSize; j++) {
							Card c = currentHand[j];
							if (c == null) continue;
							
							Colour colourKey = c.getColour();
							int valueKey = c.getValue();

							// check colour hasn't already been hinted
							if (gameState.colours[hintee][j] == null) {
								if (col.containsKey(colourKey)) {
									col.put(colourKey, col.get(colourKey) + 1);
								} else {
									col.put(colourKey, 1);
								}
							}

							// check value hasn't already been hinted
							if (gameState.values[hintee][j] == 0) {
								if (val.containsKey(valueKey)) {
									val.put(valueKey, val.get(valueKey) + 1);
								} else {
									val.put(valueKey, 1);
								}
							}
						}

						for (Entry<Colour, Integer> c : col.entrySet()) {
							if (mostCommon < c.getValue()) {
								cKey = c.getKey();
								bestHintee = hintee;
								mostCommon = c.getValue();
							}
						}

						for (Entry<Integer, Integer> v : val.entrySet()) {
							if (mostCommon < v.getValue()) {
								colour = false;
								vKey = v.getKey();
								bestHintee = hintee;
								mostCommon = v.getValue();
							}
						}
					}
					if (colour && cKey != null) {
						boolean[] results = new boolean[gameState.values[playerIndex].length];
						Card[] bestHand = gameState.hands[bestHintee];

						for (int k = 0; k < gameState.values[playerIndex].length; k++) {
							if (bestHand[k] == null) continue;
							results[k] = cKey.equals(bestHand[k].getColour());
						}
						
						possibleMoves.add(new Action(playerIndex, "Agent21750965", ActionType.HINT_COLOUR, bestHintee, results, cKey));
					} 
					else if (vKey != -1) {
						boolean[] results = new boolean[gameState.values[playerIndex].length];
						Card[] bestHand = gameState.hands[bestHintee];

						for (int k = 0; k < gameState.values[playerIndex].length; k++) {
							if (bestHand[k] == null) continue;
							results[k] = (vKey == bestHand[k].getValue());
						}
						
						possibleMoves.add(new Action(playerIndex, "Agent21750965", ActionType.HINT_VALUE, bestHintee, results, vKey));
					}
				}
			} 
			catch (IllegalActionException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Calculates and returns the Upper Confidence Bound for this Node.
		 * 
		 * @param coefficient - The exploration coefficient.
		 * @return the double UCB value for this Node.
		 */
		public double getUCB(double coefficient) {
			return ((score / visits) + (coefficient * Math.sqrt(2 * Math.log(this.availability) / visits)));
		}

		/**
		 * Checks if the possibleMoves list contains any
		 * Action that is legal given the current GameState d
		 * 
		 * @param d the current GameState
		 * @return boolean true if legal Actions exist, false otherwise
		 * @throws IllegalActionException if an illegal Action is applied to the GameState
		 */
		public boolean hasMoves(GameState d) throws IllegalActionException {
			for (Action a : possibleMoves) {
				if (d.legalAction(a)) return true;
			}
			
			return false;
		}
	}
	
	/**
	 * Original author - Tim French
	 * 
	 * Represents the state of a game of Hanabi.
	 * 
	 * @author Mark Boon
	 */
	class GameState implements Cloneable {
		
		// The deck of cards for this determinization
		public Stack<Card> deck;
		// The number of players in the game
		public int playerCount;
		// The number of cards in each player's hand
		public int handSize;
		// The number of turns Cards have been held for each player
		public int[][] ages;
		// The available Colour hint information for each player
		public Colour[][] colours;
		// The available value hint information for each player
		public int[][] values;
		
		public Random rand = new Random();

		/** The name of each of the players in the game **/
		public String[] players;
		/** The stack of cards that have been discarded, or incorrectly played **/
		public Stack<Card> discards;
		/** For each colour, the cards making up that firework so far **/
		public Map<Colour, Stack<Card>> fireworks;
		/** The hand of each player **/
		public Card[][] hands;
		/** The order of this state in the game **/
		public int order = 0;
		/** The number of hints remaining **/
		public int hints = 0;
		/** The number of fuse tokens left **/
		public int fuse = 0;
		/** The index of the next player to move **/
		public int nextPlayer = -1;
		/** The final play of the game (for when the deck runs out) **/
		public int finalAction = -1;

		/**
		 * Original author - Tim French.
		 * 
		 * A constructor for the first state in the game
		 * 
		 * @param playerCount the number of players in the game
		 **/
		public GameState(int playerCount) {
			this.playerCount = playerCount;
			fireworks = new HashMap<Colour, Stack<Card>>();
			hands = new Card[playerCount][playerCount > 3 ? 4 : 5];
			handSize = hands[0].length;
			deck = new Stack<Card>();
			ages = new int[playerCount][handSize];
			values = new int[playerCount][handSize];
			colours = new Colour[playerCount][handSize];
		}

		/**
		 * Determines a possible matching for the Agent's 
		 * hand, given the deck of unseen cards and the current
		 * hint information available.
		 */
		public void determinePlayerHand() {
			// Flags if we need to perform complex assignment
			boolean complexAssign = false;
			// The resulting player hand
			Card[] playerHand = new Card[handSize];
			// LinkedList representing slots in the player's hand
			LinkedList<CardSlot> cardSlots = new LinkedList<CardSlot>();
			// The list of all possible Cards in all slots
			ArrayList<Card> totalPossibilities = new ArrayList<Card>();

			for (int i = 0; i < handSize; i++) {
				cardSlots.add(new CardSlot(i));

				// Remove any cards that are determined from full hint information
				if (colours[nextPlayer][i] != null && values[nextPlayer][i] != 0) {
					playerHand[i] = new Card(colours[nextPlayer][i], values[nextPlayer][i]);
					cardSlots.removeLast();
					deck.remove(playerHand[i]);
				}
			}
			
			for (Card c : deck) {
				for (CardSlot cs : cardSlots) {
					if (c.getColour().equals(colours[nextPlayer][cs.index])) {
						cs.possibleCardsList.add(c);
						cs.possibleCardsSet.add(c);
						totalPossibilities.add(c);
					}
					else if (values[nextPlayer][cs.index] == c.getValue()) {
						cs.possibleCardsList.add(c);
						cs.possibleCardsSet.add(c);
						totalPossibilities.add(c);
					}
					else if (colours[nextPlayer][cs.index] == null && values[nextPlayer][cs.index] == 0) {
						cs.possibleCardsList.add(c);
						cs.possibleCardsSet.add(c);
						totalPossibilities.add(c);
					}
				}
			}
			
			for (CardSlot cs : cardSlots) {
				// This handles the edge case that we hold a null
				if (cs.possibleCardsSet.size() == 0) {
					cs.assigned = true;
				}
				
				// Check if we need complex assignment 
				// by using the pigeon-hole principle
				if (!cs.assigned && cs.possibleCardsList.size() < handSize) {
					complexAssign = true;
				}
			}
			
			if (!complexAssign) {
				// Iterate through cardSlots and
				// assign cards at random.
				for (CardSlot cs : cardSlots) {
					if (cs.assigned) continue;
					Card c = cs.possibleCardsList.get(rand.nextInt((cs.possibleCardsList.size())));
					
					for (CardSlot clean : cardSlots) {
						clean.possibleCardsList.remove(c);
					}
					
					playerHand[cs.index] = c;
				}
			}
			else {
				boolean[][] graph = new boolean[cardSlots.size()][totalPossibilities.size()];
				
				// Construct an adjacency matrix between hand positions and possibilities 
				for (int i = 0; i < cardSlots.size(); i++) {
					for (int j = 0; j < totalPossibilities.size(); j++) {
						graph[i][j] = cardSlots.get(i).possibleCardsSet.contains(totalPossibilities.get(j));
					}
				}
				
				int[] matchIndex = solveHand(graph, cardSlots.size(), totalPossibilities.size());
				
				// Assign Cards to hand positions with the results
				// stored in the matchIndex array
				for (int i = 0; i < matchIndex.length; i++) {
					if (matchIndex[i] == -1) continue;
					CardSlot cs = cardSlots.get(matchIndex[i]);
					playerHand[cs.index] = totalPossibilities.get(i);
				}
			}
					
			// Set the Agent's hand equal to playerHand
			hands[nextPlayer] = (Card[]) playerHand.clone();
		}

		/**
		 * Begins the Maximum Bipartite Matching on the adjacency matrix
		 * of card positions and card possibilities. Positions are indexed 
		 * at random to provide variation in the assignment outcome.
		 * 
		 * @param graph the adjacency matrix 
		 * @param slotCount the number of hand positions to fill
		 * @param cardCount the number of possible Cards
		 * @return an array containing indices matching hand positions
		 * to Card possibilities 
		 */
		public int[] solveHand(boolean[][] graph, int slotCount, int cardCount) {
	        int matchIndex[] = new int[cardCount];
	        ArrayList<Integer> randomOrder = new ArrayList<Integer>(slotCount);
	        
	        for (int i = 0; i < slotCount; i++) {
	        	randomOrder.add(i);
	        }
	        
	        Collections.shuffle(randomOrder);
	  
	        Arrays.fill(matchIndex, -1);
	  
	        for (int i : randomOrder) { 
	            boolean seen[] = new boolean[cardCount] ; 
	            match(graph, i, seen, matchIndex, cardCount);
	        } 
	        
	        return matchIndex; 
		}

		/**
		 * Performs a recursive DFS based algorithm to 
		 * calculate the Maximum Bipartite Matching. 
		 * 
		 * @param graph the adjacency matrix to calculate
		 * @param index the index of the current hand position
		 * @param seen a list of indices that have already been visited
		 * @param matchIndex an array of the current matching indices
		 * @param cardCount the number of possible cards
		 * @return true if a matching occurred, false otherwise
		 */
		private boolean match(boolean[][] graph, int index, boolean[] seen, int[] matchIndex, int cardCount) {
			ArrayList<Integer> randomOrder = new ArrayList<Integer>(cardCount);
	        
	        for (int i = 0; i < cardCount; i++) {
	        	randomOrder.add(i);
	        }
	        
	        Collections.shuffle(randomOrder);
	        
	        for (int i : randomOrder) { 
	            if (graph[index][i] && !seen[i]) { 
	                // Mark i as visited 
	                seen[i] = true;  
	  
	                if (matchIndex[i] < 0 || match(graph, matchIndex[i], seen, matchIndex, cardCount)) { 
	                    matchIndex[i] = index; 
	                    return true; 
	                } 
	            } 
	        } 
	        
	        return false;
		}

		/**
		 * Original author - Tim French.
		 * 
		 * A method to create the next state from the given state and a move.
		 *
		 * @param action the Action to be applied
		 * @throws IllegalActionException if the move is not legal in the current state
		 **/
		public GameState nextState(Action action) throws IllegalActionException {
			if (!legalAction(action)) throw new IllegalActionException("Invalid action!: " + action);
			if (gameOver()) throw new IllegalActionException("Game Over!");
			
			GameState s = (GameState) this.clone();
			
			switch (action.getType()) {
			case PLAY:
				int player = action.getPlayer();
				int card = action.getCard();
				
				// Update hint and age information
				s.ages[player][card] = 0;
				s.colours[player][card] = null;
				s.values[player][card] = 0;
				
				Card c = hands[player][card];
				Stack<Card> fw = fireworks.get(c.getColour());
				if ((fw.isEmpty() && c.getValue() == 1) || (!fw.isEmpty() && fw.peek().getValue() == c.getValue() - 1)) {
					s.fireworks.get(c.getColour()).push(c);
					if (s.fireworks.get(c.getColour()).size() == 5 && s.hints < 8)
						s.hints++;
				} else {
					s.discards.push(c);
					s.fuse--;
				}
				if(!deck.isEmpty()) s.hands[action.getPlayer()][action.getCard()] = deck.pop();
			    else s.hands[action.getPlayer()][action.getCard()] = null;
			    if(deck.isEmpty() && finalAction==-1) s.finalAction = order+players.length;

				break;
			case DISCARD:
				player = action.getPlayer();
				card = action.getCard();
				
				// Update hint and age information
				s.ages[player][card] = 0;
				s.colours[player][card] = null;
				s.values[player][card] = 0;
				
				c = hands[action.getPlayer()][action.getCard()];
				s.discards.push(c);
		        if(!deck.isEmpty()) s.hands[action.getPlayer()][action.getCard()] = deck.pop();
		        else s.hands[action.getPlayer()][action.getCard()] = null;
		        if(deck.isEmpty() && finalAction==-1) s.finalAction = order+players.length;
		        if(hints<8) s.hints++;
				break;
			case HINT_COLOUR:
				boolean[] colourHints = action.getHintedCards();
				for (int i = 0; i < colourHints.length; i++) {
					// Update Colour hint information
					if (colourHints[i]) s.colours[action.getHintReceiver()][i] = action.getColour();
				}
				s.hints--;
				break;
			case HINT_VALUE:
				boolean[] valueHints = action.getHintedCards();
				for (int i = 0; i < valueHints.length; i++) {
					// Update value hint information
					if (valueHints[i]) s.values[action.getHintReceiver()][i] = action.getValue();
				}
				s.hints--;
				break;
			default:
				break;
			}
			s.order++;
			s.nextPlayer = (nextPlayer + 1) % players.length;
			return s;
		}

		/**
		 * Original author - Tim French.
		 * 
		 * Test the legality of a Action. If the observer of a state is specified,
		 * this method can only be applied to actions performed by the observer.
		 * 
		 * @param a the move to be tested
		 * @return true if the move is legal in the current game state.
		 * @throws IllegalActionException 
		 **/
		public boolean legalAction(Action a) throws IllegalActionException {
			if (a.getPlayer() != nextPlayer) return false;
			switch (a.getType()) {
			case PLAY:
				return (a.getCard() >= 0 && a.getCard() < hands[nextPlayer].length);
			case DISCARD:
				if (hints == 8) return false;
				return (a.getCard() >= 0 && a.getCard() < hands[nextPlayer].length);
			case HINT_COLOUR:
				if (hints == 0 || a.getHintReceiver() < 0 || a.getHintReceiver() > players.length || a.getHintReceiver() == a.getPlayer()) return false;
				boolean[] hint = new boolean[hands[a.getHintReceiver()].length];
				for (int i = 0; i < hint.length; i++) {
					Card c = hands[a.getHintReceiver()][i];
					hint[i] = (c == null ? null : c.getColour()) == a.getColour();
				}
				return Arrays.equals(hint, a.getHintedCards());
			case HINT_VALUE:
				if (hints == 0 || a.getHintReceiver() < 0
						|| a.getHintReceiver() > players.length
						|| a.getHintReceiver() == a.getPlayer())
					return false;
				hint = new boolean[hands[a.getHintReceiver()].length];
				for (int i = 0; i < hint.length; i++) {
					Card c = hands[a.getHintReceiver()][i];
					hint[i] = (c == null ? -1 : c.getValue()) == a.getValue();
				}
				return Arrays.equals(hint, a.getHintedCards());
			default:
				return false;
			}
		}

		/**
		 * Original Author - Tim French.
		 * 
		 * Get the stack of cards representing the specified firework
		 * 
		 * @return a clone of the stack of cards representing the firework of the
		 *         given colour. The highest card is at the top of the stack.
		 **/
		@SuppressWarnings("unchecked")
		public Stack<Card> getFirework(Colour c) {
			return (Stack<Card>) fireworks.get(c).clone();
		}

		/**
		 * Original Author - Tim French.
		 * 
		 * Get the current score
		 * 
		 * @return the sum of the highest value cards in each firework
		 **/
		public int getScore() {
			if (fuse == 0) return 0;
			int score = 0;
			for (Colour c : Colour.values())
				if (!fireworks.get(c).isEmpty()) score += fireworks.get(c).peek().getValue();
			return score;
		}

		/**
		 * Original author - Tim French.
		 * 
		 * Tests if the game is over.
		 * 
		 * @return true if all fireworks have been made, 
		 * the deck has run out, or a fuse has exploded.
		 **/
		public boolean gameOver() {
			return ((finalAction != -1 && order == finalAction + 1) || fuse == 0 || getScore() == 25);
		}

		/**
		 * Original author - Tim French.
		 * 
		 * Produces a clone of the GameState.
		 * 
		 * @return an Object clone of the GameState
		 **/
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public Object clone() {
			try {
				GameState s = (GameState) super.clone();
				s.players = players.clone();
				s.discards = (Stack<Card>) discards.clone();
				s.hands = (Card[][]) hands.clone();
				
				for (int i = 0; i < hands.length; i++)
					s.hands[i] = (Card[]) s.hands[i].clone();
				
				s.fireworks = (Map<Colour, Stack<Card>>) ((HashMap) fireworks).clone();
				s.colours = (Colour[][]) colours.clone();
				s.values = (int[][]) values.clone();
				
				for (int i = 0; i < playerCount; i++) {
					s.colours[i] = (Colour[]) colours[i].clone();
					s.values[i] = (int[]) values[i].clone();
				}
				
				for (Colour c : Colour.values())
					s.fireworks.put(c, (Stack<Card>) fireworks.get(c).clone());
				
				return s;
			} 
			catch (CloneNotSupportedException e) {
				return null;
			}
		}

		/**
		 * Original Author - Tim French.
		 * 
		 * Returns the first playable Action in a player's hand,
		 * or null if no playable Actions exist.
		 * 
		 * @return the first playable Action
		 * @throws IllegalActionException if an illegal Action is created
		 */
		public Action playKnown() throws IllegalActionException {
			for (int i = 0; i < handSize; i++) {
				if (colours[nextPlayer][i] != null && values[nextPlayer][i] == playable(colours[nextPlayer][i])) {
					return new Action(nextPlayer, "Agent21750965", ActionType.PLAY, i);
				}
			}
			
			return null;
		}

		/**
		 * Original author - Tim French.
		 * 
		 * Returns the next playable value of a fireworks 
		 * stack or -1 if the stack is complete.
		 * 
		 * @param colour The Colour of the fireworks stack to inspect
		 * @return The next playable value of the fireworks stack, else -1
		 */
		public int playable(Colour colour) {
			java.util.Stack<Card> fw = getFirework(colour);
			if (fw.size() == 5) return -1;
			else return fw.size() + 1;
		}

		/**
		 * Original author - Tim French.
		 * 
		 * Calculates and returns the first discard 
		 * Action for a Card known to be useless,
		 * 
		 * @return the DISCARD Action for the first useless card, or null if none exists
		 * @throws IllegalActionException if an illegal Action is created
		 */
		public Action discardKnown() throws IllegalActionException {
			if (hints != 8) {
				for (int i = 0; i < handSize; i++) {
					if (colours[nextPlayer][i] != null && values[nextPlayer][i] > 0 && values[nextPlayer][i] < playable(colours[nextPlayer][i])) {
						return new Action(nextPlayer, "Agent21750965", ActionType.DISCARD, i);
					}
				}
			}
			
			return null;
		}

		/**
		 * Calculates and returns a hint Action for the first 
		 * unhinted Card known to be playable. Value hints are
		 * prioritized over Colour hints.
		 * 
		 * @return a hint Action for the first playable Card
		 * or null if no playable Card is found
		 * @throws IllegalActionException if an illegal Action is created
		 */
		public Action hintPlayable() throws IllegalActionException {
			if (hints > 0) {
				for (int i = 1; i < playerCount; i++) {
					int hintee = (nextPlayer + i) % playerCount;
					Card[] hand = hands[hintee];

					for (int j = 0; j < hand.length; j++) {
						Card c = hand[j];
						
						if (c != null && c.getValue() == playable(c.getColour())) {
							if (values[hintee][j] == 0) {
								boolean[] val = new boolean[hand.length];

								for (int k = 0; k < val.length; k++) {
									if (hand[k] == null) continue;
									val[k] = c.getValue() == hand[k].getValue();
								}
								
								return new Action(nextPlayer, "Agent21750965", ActionType.HINT_VALUE, hintee, val, c.getValue());
							} 
							else if (colours[hintee][j] == null) {
								boolean[] col = new boolean[hand.length];

								for (int k = 0; k < col.length; k++) {
									if (hand[k] == null) continue;
									col[k] = c.getColour().equals(hand[k].getColour());
								}
								
								return new Action(nextPlayer, "Agent21750965", ActionType.HINT_COLOUR, hintee, col, c.getColour());
							}
						}
					}
				}
			}
			
			return null;
		}

		/**
		 * Calculates and returns a hint Action that
		 * reveals the greatest volume of unhinted information.
		 * 
		 * @return the hint Action revealing the most information or 
		 * null if no information revealing hint is possible
		 * @throws IllegalActionException if an illegal Action is created
		 */
		public Action hintMostCommon() throws IllegalActionException {
			int mostCommon = -100;
			int bestHintee = -1;
			boolean colour = true;
			Colour cKey = null;
			int vKey = -1;

			if (hints > 0) {
				for (int i = 1; i < playerCount; i++) {
					int hintee = (nextPlayer + i) % playerCount;
					if (hintee == nextPlayer) continue;
					
					Map<Colour, Integer> col = new HashMap<Colour, Integer>();
					Map<Integer, Integer> val = new HashMap<Integer, Integer>();
					Card[] hand = hands[hintee];

					for (int j = 0; j < hand.length; j++) {
						Card c = hand[j];
						if (c == null) continue;
						Colour colourKey = c.getColour();
						int valueKey = c.getValue();

						// check colour hasn't already been hinted
						if (colours[hintee][j] == null) {
							if (col.containsKey(colourKey)) {
								col.put(colourKey, col.get(colourKey) + 1);
							} else {
								col.put(colourKey, 1);
							}
						}

						// check value hasn't already been hinted
						if (values[hintee][j] == 0) {
							if (val.containsKey(valueKey)) {
								val.put(valueKey, val.get(valueKey) + 1);
							} else {
								val.put(valueKey, 1);
							}
						}
					}

					for (Entry<Colour, Integer> c : col.entrySet()) {
						if (mostCommon < c.getValue()) {
							cKey = c.getKey();
							bestHintee = hintee;
							mostCommon = c.getValue();
						}
					}

					for (Entry<Integer, Integer> v : val.entrySet()) {
						if (mostCommon < v.getValue()) {
							colour = false;
							vKey = v.getKey();
							bestHintee = hintee;
							mostCommon = v.getValue();
						}
					}
				}

				if (colour && bestHintee != -1) {
					boolean[] results = new boolean[handSize];
					Card[] bestHand = hands[bestHintee];

					for (int k = 0; k < values[nextPlayer].length; k++) {
						if (bestHand[k] == null) continue;
						results[k] = cKey.equals(bestHand[k].getColour());
					}

					return new Action(nextPlayer, "Agent21750965", ActionType.HINT_COLOUR, bestHintee, results, cKey);
				} 
				else if (bestHintee != -1) {
					boolean[] results = new boolean[values[nextPlayer].length];
					Card[] bestHand = hands[bestHintee];

					for (int k = 0; k < values[nextPlayer].length; k++) {
						if (bestHand[k] == null) continue;
						results[k] = (vKey == bestHand[k].getValue());
					}

					return new Action(nextPlayer, "Agent21750965", ActionType.HINT_VALUE, bestHintee, results, vKey);
				}
			}
			
			return null;
		}

		/**
		 * Calculates and returns the Action that discards
		 * the oldest Card in the current player's hand.
		 * 
		 * @return a discard Action for the oldest Card in nextPlayer's hand
		 * @throws IllegalActionException if an illegal Action is created
		 */
		public Action discardOldest() throws IllegalActionException {
			if (hints != 8) {
				int oldest = ages[nextPlayer][0];
				int discardIndex = 0;

				for (int i = 1; i < handSize; i++) {
					if (ages[nextPlayer][i] > oldest && values[nextPlayer][i] != 5) {
						oldest = ages[nextPlayer][i];
						discardIndex = i;
					}
				}
				
				return new Action(nextPlayer, "Agent21750965", ActionType.DISCARD, discardIndex);
			}
			
			return null;
		}
	}
	
	/**
	 * A class to represent a Card position
	 * in the hand of a player.
	 * 
	 * @author Mark Boon
	 */
	class CardSlot {
		
		// Represents the index in playerHand this slot corresponds to
		public int index;
		// Represents if the slot has been assigned to playerHand
		public boolean assigned;
		// A set of possible Cards for this slot 
		// to exploit the O(1) lookup time
		public HashSet<Card> possibleCardsSet;
		// A list of possible Cards for this slot
		public ArrayList<Card> possibleCardsList;
		
		/**
		 * Default constructor.
		 * 
		 * @param i the hand position index of this CardSlot
		 */
		public CardSlot(int i) {
			index = i;
			assigned = false;
			possibleCardsSet = new HashSet<Card>(); 
			possibleCardsList = new ArrayList<Card>(); 
		}
	}

	/**
	 * A class representing an expanded Node and 
	 * the corresponding GameState in IS-MCTS+UCB
	 * @author Mark Boon
	 *
	 * @param <Node>
	 * @param <GameState>
	 */
	@SuppressWarnings("hiding")
	class Result<Node, GameState> {
		public Node n;
		public GameState d;
		
		/**
		 * Default constructor.
		 * 
		 * @param n the expanded Node
		 * @param d the corresponding GameState
		 */
		public Result(Node n, GameState d) {
			this.n = n;
	        this.d = d;
		}
	}
}
