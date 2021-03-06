package player;

import java.util.ArrayList;
import java.util.List;

import world.ConfigurationCounter;
import world.OppWorld;
import world.World;
import world.OppWorld.cellState;
import world.World.Coordinate;

/**
 * Monte Carlo guess player (task C).
 * Please implement this class.
 *
 * @author Youhan, Jeffrey
 */
public class MonteCarloGuessPlayer extends Guesser implements Player{

	public static final int BOARD_EDGE = 0;
	public static final int CURRENT_CONFIGURATION = 1;
	public static final int INCLUSIVE_EXTRA_CELL = 1;
	public boolean hitButNotSunk;
	public Guess firstHit;
	public int directionLength;
	public Direction direction;
	
	@Override
	public void initialisePlayer(World world) {
		this.myWorld = world;
		this.opponentsWorld = new OppWorld( world.numRow, world.numColumn, true );
		this.hitsToMyFleet = new ArrayList<>();
		this.opponentsWorld.initialiseShipCounters();
		this.hitButNotSunk = false;
		this.direction = Direction.NORTH;
		this.directionLength = 1;
		initialiseTotalCountToZero(opponentsWorld.total);
		setupAllConfigurations(opponentsWorld.ShipCounters, opponentsWorld.total);
	} // end of initialisePlayer()

	public void initialiseTotalCountToZero(ConfigurationCounter board){
		for(int y = 0; y < board.rows; ++y){ // for each row
			for(int x = 0; x < board.columns; ++x){ // for each column
				board.ShipConfigurationCounts[y][x] = 0;
			}
		}
	}

	public void setupAllConfigurations(List<ConfigurationCounter> shipCounters, ConfigurationCounter total) {
		for(ConfigurationCounter board: shipCounters){ // for each counter
			for(int y = 0; y < board.rows; ++y){ // for each row
				for(int x = 0; x < board.columns; ++x){ // for each cell
					//Count the configurations
					int configurations = getShipConfigurationCountForOneCell(y, x, board.shipSize);
					//Store the result in the board
					board.ShipConfigurationCounts[y][x] = configurations;
					//Update the total Count for that cell
					total.ShipConfigurationCounts[y][x] += configurations;
				}
			}
		}	
	}// end of countAllConfigurations()

	public int getShipConfigurationCountForOneCell(int column, int row, int shipSize) {
		int rowMax = getUpperBoundForRow(column, row, shipSize);
		int rowMin = getLowerBoundForRow(column, row, shipSize);
		int colMax = getUpperBoundForColumn(column, row, shipSize);
		int colMin = getLowerBoundForColumn(column, row, shipSize);
		// calculate the total number of cells available in the row and col
		int rowRange = rowMax - rowMin - INCLUSIVE_EXTRA_CELL;
		int colRange = colMax - colMin - INCLUSIVE_EXTRA_CELL;
		// calculate how many ways a ship can be placed in that space
		int rowConfigurations = max(0, CURRENT_CONFIGURATION + rowRange - shipSize);
		int colConfigurations = max(0, CURRENT_CONFIGURATION + colRange - shipSize);
		return rowConfigurations + colConfigurations;
	}

	public int getLowerBoundForColumn(int column, int row, int shipSize) {
		for(int y = 1; y < shipSize; ++y){ //cells below in the column
			if(isOutOfBounds(column, row - y) || isObstacle(column, row - y))
				return row - y; 
		}
		return row - shipSize; // Bound is excluded from the range
	}

	public int getUpperBoundForColumn(int column, int row, int shipSize) {
		for(int y = 1; y < shipSize; ++y){ //cells above in the column
			if(isOutOfBounds(column, row + y) || isObstacle(column, row + y))
				return row + y; 
		}
		return row + shipSize; // Bound is excluded from the range
	}

	public int getLowerBoundForRow(int column, int row, int shipSize) {
		for(int x = 1; x < shipSize; ++x){ //cells below in the row
			if(isOutOfBounds(column - x, row) || isObstacle(column - x, row))
				return column - x; 
		}
		return column - shipSize; // Bound is excluded from the range
	}

	public int getUpperBoundForRow(int column, int row, int shipSize) {
		for(int x = 1; x < shipSize; ++x){ //cells above in the row
			if(isOutOfBounds(column + x, row) || isObstacle(column + x, row))
				return column + x; 
		}
		return column + shipSize; // Bound is excluded from the range
	}

	@Override
	public Guess makeGuess() {
		Guess g = null;
		// **Targeting Mode
		if (hitButNotSunk){
			while(g == null){ // each loop represents a direction
				g = getNextTarget(this.firstHit); //go in one direction for the next guess
				directionLength++; // increment for the next guess 
				// if that direction is a dead end
				if(g == null || isOutOfBounds(g.row, g.column) || isObstacle(g.row, g.column)){
					g = null;
					direction = direction.next();
					directionLength = 1;
				}
			}
		}
		// **Hunting Mode**
		else{
			g = new Guess();
			//clear the possible targets from the last ship
			opponentsWorld.possibleTargets.clear(); 
			opponentsWorld.resetAllPossibleTargets();
			// look for the cell with the highest configuration count
			int highestCount = 0;
			for(int y = 0; y < opponentsWorld.numRows; ++y){ //for each row
				for(int x = 0; x < opponentsWorld.numColumns; ++x){ // for each column
					// if it is a higher number, make it the new guess
					if(opponentsWorld.total.ShipConfigurationCounts[y][x] > highestCount){
						highestCount = opponentsWorld.total.ShipConfigurationCounts[y][x];
						g.column = x;
						g.row = y;
					}
				}
			}
		}
		return g;
	} // end of makeGuess()
	
	public Guess getNextTarget(Guess centre) {
		Guess g = new Guess();
		g.column = centre.column;
		g.row = centre.row;
		switch(direction){
			case NORTH:
				g.row = g.row + directionLength;
				break;
			case EAST:
				g.column = g.column + directionLength;
				break;
			case WEST:
				g.column = g.column - directionLength;
				break;
			case SOUTH:
				g.row = g.row - directionLength;
				break;
		}
		return g;
	}

	public Guess getPossibleTargetWithHighestCount(){
		int highestCount = 0;
		//initialise the coordinate to the first element in the list to guarantee return value
		Coordinate largest = opponentsWorld.possibleTargets.get(0);
		//find the coordinate with the largest count
		for(Coordinate c: opponentsWorld.possibleTargets){
			if(opponentsWorld.total.ShipConfigurationCounts[c.row][c.column] > highestCount){
				largest = c;
				highestCount = opponentsWorld.total.ShipConfigurationCounts[c.row][c.column];
			}
		}
		// remove it from the list
		opponentsWorld.possibleTargets.remove(largest);
		// return it as a guess object
		return createGuess(largest);
	}
	
	@Override
	public void update(Guess guess, Answer answer) {
		// Check actions for a hit
		if(answer.isHit){
			if(firstHit == null){
				firstHit = guess;
			}
			opponentsWorld.updateCell ( cellState.Hit, guess.row, guess.column );
			hitButNotSunk = true;
			if(answer.shipSunk != null){
				firstHit = null;
				hitButNotSunk = false;
				directionLength = 1;
			}
		}
		else{
			if(hitButNotSunk){
				direction = direction.next();
				directionLength = 1;
			}
			opponentsWorld.updateCell ( cellState.Miss, guess.row, guess.column );
		}
		
		// Check actions if is ship sunk
		updateConfigurationCount(guess);
		recalculateTotalCount();
	}

	public void recalculateTotalCount() {
		//reset all counts to zero
		initialiseTotalCountToZero(opponentsWorld.total);
		//iterate through every cell in shipCounters and accumulate a total
		for(ConfigurationCounter shipCounter: opponentsWorld.ShipCounters){ // for each ship
			for(int y = 0; y < myWorld.numRow; ++y){ // for each row
				for(int x = 0; x < myWorld.numColumn; ++x){ // for each column
					opponentsWorld.total.ShipConfigurationCounts[y][x] +=
							shipCounter.ShipConfigurationCounts[y][x];
				}
			}
		}
	}

	public void updateConfigurationCount(Guess guess) {
		//for each shipCounter
		for(ConfigurationCounter shipCounter: opponentsWorld.ShipCounters){
			//set shot to zero
			shipCounter.ShipConfigurationCounts[guess.row][guess.column] = 0;
			// travel each direction to update counts
			updateRowUpper(guess, shipCounter);
			updateRowLower(guess, shipCounter);
			updateColumnUpper(guess, shipCounter);
			updateColumnLower(guess, shipCounter);
		}
	}

	public void updateRowUpper(Guess guess, ConfigurationCounter shipCounter) {
		// visit each cell above the shot in the row and recalculate the count
		for(int x = 1; x < shipCounter.shipSize; ++x){ // start at 1 to avoid the shot cell
			// if there is an obstacle, stop
			if(isOutOfBounds(guess.row, guess.column + x)
					|| isObstacle(guess.row, guess.column + x))
				break;
			else // recalculate the configurations
				shipCounter.ShipConfigurationCounts[guess.row][guess.column + x] =
				getShipConfigurationCountForOneCell(guess.row, guess.column + x, shipCounter.shipSize);
		}
	}

	public void updateRowLower(Guess guess, ConfigurationCounter shipCounter) {
		// visit each cell below the shot in the row and recalculate the count
		for(int x = 1; x < shipCounter.shipSize; ++x){ 
			// if there is an obstacle, stop
			if(isOutOfBounds(guess.row, guess.column - x)
					|| isObstacle(guess.row, guess.column - x))
				break;
			else // recalculate the configurations
				shipCounter.ShipConfigurationCounts[guess.row][guess.column - x] =
				getShipConfigurationCountForOneCell(guess.row, guess.column - x, shipCounter.shipSize);
		}
	}

	public void updateColumnUpper(Guess guess, ConfigurationCounter shipCounter){
		//visit each cell above the shot in the column and recalculate the count
		for(int y = 1; y < shipCounter.shipSize; ++y){
			// if there is an obstacle, stop
			if(isOutOfBounds(guess.row + y, guess.column) 
					|| isObstacle(guess.row + y, guess.column))
				break;
			else // recalculate the configurations
				shipCounter.ShipConfigurationCounts[guess.row + y][guess.column] =
				getShipConfigurationCountForOneCell(guess.row + y, guess.column, shipCounter.shipSize);
		}
	}
	
	public void updateColumnLower(Guess guess, ConfigurationCounter shipCounter){
		//visit each cell below the shot in the column and recalculate the count
		for(int y = 1; y < shipCounter.shipSize; ++y){
			// if there is an obstacle, stop
			if(isOutOfBounds(guess.row - y, guess.column)
					|| isObstacle(guess.row - y, guess.column))
				break;
			else // recalculate the configurations
				shipCounter.ShipConfigurationCounts[guess.row - y][guess.column] =
				getShipConfigurationCountForOneCell(guess.row - y, guess.column, shipCounter.shipSize);
		}
	}

	public boolean isObstacle(int y, int x) {
		return (opponentsWorld.oppWorld[y][x] == cellState.Miss 
				|| opponentsWorld.oppWorld[y][x] == cellState.Hit);
	}
	
	public boolean isOutOfBounds(int y, int x) {
		return (y < BOARD_EDGE
				|| y >= myWorld.numRow
				|| x < BOARD_EDGE
				|| x >= myWorld.numColumn);
	}

	private int max(int x, int y) {
		if(x > y)
			return x;
		else
			return y;
	}

} // end of class MonteCarloGuessPlayer
