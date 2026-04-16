package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import com.google.common.collect.ImmutableSet;
import jakarta.annotation.Nonnull;

import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * cw-model
 * Stage 2: Complete this class
 */
//hold a game state and observer list
public final class MyModelFactory implements Factory<Model> {

	@Nonnull @Override public Model build(GameSetup setup,
										  Player mrX,
										  ImmutableList<Player> detectives) {

		// Anonymous inline class implementing Model
		return new Model() {
			// You need to implement ALL methods from the Model interface here


			private Board.GameState currentState = new MyGameStateFactory().build(setup, mrX, detectives);
			private List<Board.GameState> gameStates = new ArrayList<>(List.of(currentState));
			private Set<Observer> observers = new HashSet<>();


			@Nonnull public Board getCurrentBoard(){
				return this.currentState;
			}
			public void registerObserver(@Nonnull Observer observer){
				if (observer==null){
					throw new NullPointerException("observer shouldn't be null");
				}
				if (!observers.add(observer)) {
					throw new IllegalArgumentException("Observer already registered");
				}
			}
			public void unregisterObserver(@Nonnull Observer observer){
				if (observer==null){
					throw new NullPointerException("observer shouldn't be null");
				}
				if (!observers.remove(observer)) {
					throw new IllegalArgumentException("Observer not found");
				}
			}
			@Nonnull
			public ImmutableSet<Observer> getObservers(){
				return ImmutableSet.copyOf(observers);
			}
			// the below method is called when a move has been selected in the GUI

			public void chooseMove(@Nonnull Move move){
				this.currentState=currentState.advance(move);
				this.gameStates.add(this.currentState);
				Observer.Event event;
				if(this.currentState.getWinner().isEmpty()){
					event= Observer.Event.MOVE_MADE;
				}else{
					event= Observer.Event.GAME_OVER;
				}
				eventPasser(event);
			}
			private void eventPasser(Observer.Event event){
				for (Observer o:observers){
					o.onModelChanged(this.currentState,event);
				}
			}
			// Add all other required Model interface methods...

		};
	}
	//define a new class that implements model a concrete subject


}
