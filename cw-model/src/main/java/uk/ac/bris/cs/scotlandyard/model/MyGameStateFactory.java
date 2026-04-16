package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;


import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import jakarta.annotation.Nonnull;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import java.util.Objects;
import java.util.Optional;
import java.util.*;
import uk.ac.bris.cs.scotlandyard.model.Move.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;
/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {

	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {

		return new MyGameState(setup, ImmutableSet.of(MrX.MRX), ImmutableList.of(), mrX, Lists.newArrayList(detectives),null,true,ImmutableList.of());
	}

		//The nested class doesn't need access to the outer class's instance variables

		//You want to create instances without needing an outer class instance
		private static final class MyGameState implements GameState {
			private final GameSetup setup;
			private Player mrX;
			private final 	List<Player> detectives;
			//private final ImmutableMap<Piece.Detective, Integer> detectiveLocations;
			private ImmutableList<LogEntry> travelLog;
			private ImmutableSet<Piece> currentWinners;
			private ImmutableSet<Piece> remaining;
			private ImmutableSet<Move> moves;

			private boolean mrxTurn;
			// Constructor
			private static int roundNum=1;
			private ImmutableList<Piece> played;//adding this in to see who has played so far
			private Set<Integer> detectiveLocations;

			private MyGameState(final GameSetup setup,
							   final ImmutableSet<Piece> remaining,
							   final ImmutableList<LogEntry> log,
							   final Player mrX,
							   final List<Player> detectives,final ImmutableSet<Move> moves,final boolean mrxturn,final ImmutableList<Piece> played) {

				this.mrxTurn=mrxturn;
				this.played=played;
				this.setup = Objects.requireNonNull(setup, "setup cannot be null");
				if(setup.moves.isEmpty()) throw new IllegalArgumentException("Moves is empty!");
				this.remaining = Objects.requireNonNull(remaining, "remaining cannot be null");
				this.travelLog = Objects.requireNonNull(log, "log cannot be null");
				this.mrX = Objects.requireNonNull(mrX, "mrX cannot be null");
				if (!this.mrX.isMrX())throw new IllegalArgumentException("Mr.x missing");
				this.detectives = Objects.requireNonNull(detectives, "detectives cannot be null");
				roundNum= travelLog.size();
				Set<String> seenPlayer = new HashSet<>();
				Set<Integer> seenLocation = new HashSet<>();
				for (Player detective : this.detectives) {
					if (detective == null) {
						throw new NullPointerException("detectives list contains a null element");
					} if (detective.isMrX()){
						throw new IllegalArgumentException("2 Mr.x passed");
					}if(!seenPlayer.add(detective.piece().webColour())){
						throw new IllegalArgumentException(("duplicate players"));
					}if(!seenLocation.add(detective.location())){
						throw new IllegalArgumentException(("duplicate player locations"));
					}
					Map<Ticket, Integer> tickets = detective.tickets();
					if (tickets == null) {
						throw new IllegalArgumentException("Detective tickets map cannot be null");
					}

					if (tickets.getOrDefault(Ticket.DOUBLE, 0) != 0 ||
							tickets.getOrDefault(Ticket.SECRET, 0) != 0) {
						throw new IllegalArgumentException("Players cannot have double or secret tickets");
					}

				}
				if (this.setup.graph.nodes().isEmpty()){throw new IllegalArgumentException("setup graph shouldn't be empty");}
				this.detectiveLocations=seenLocation;
				this.moves=moves;
				this.getAvailableMoves();
				this.currentWinners=getWinner();

				if (!this.currentWinners.isEmpty()){
					this.moves=ImmutableSet.of();
				}


			}

			@Nonnull
			@Override
			public GameState advance(Move move) {

				ImmutableSet<Move> moves=this.getAvailableMoves();
				if(!moves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move);
				//visitor pattern via anonymous in line class to get the destination of move
				List<Integer> destination = move.accept(new Visitor<List<Integer>>() {
					@Override
					public List<Integer> visit(SingleMove singleMove) {
						return Collections.singletonList(singleMove.destination);
					}

					@Override
					public List<Integer> visit(DoubleMove doubleMove) {
						return Arrays.asList(doubleMove.destination1, doubleMove.destination2);
					}
				});

				//update detective
				if (!this.mrxTurn){

					for (Player p:this.detectives){
						if(p.piece().webColour().equals(move.commencedBy().webColour())){
							if(!this.played.contains(p.piece())){
								Player changing;

								changing=p.at(destination.get(0));
								Iterable<Ticket> ticketsUsed=move.tickets();
								this.mrX=this.mrX.give(ticketsUsed);
								changing=changing.use(ticketsUsed);//need to pass ticket to mrx
								ImmutableList.Builder<Piece> newPlayed = ImmutableList.builder();
								newPlayed.addAll(this.played);
								newPlayed.add(move.commencedBy());
                                this.played= newPlayed.build();
								ImmutableSet<Piece> newSet = this.remaining.stream()
										.filter(piece -> !piece.equals(p.piece()))
										.collect(ImmutableSet.toImmutableSet());
								this.detectives.remove(p);
								this.detectives.add(changing);
								this.moves=null;
								ImmutableSet<Move> newmoves=this.getAvailableMoves();
								if(!newmoves.isEmpty()){
									this.moves=newmoves;


								}
								else{
									this.moves=null;

								}

							}else{
								throw new IllegalArgumentException("player is moving twice ");
							}
							break;




						}

					}
					if (this.remaining.isEmpty()){
						this.mrxTurn=true;
						this.remaining=ImmutableSet.of();
						this.played=ImmutableList.of();
						roundNum++;
						this.moves=null;
					}
					return new MyGameState(this.setup,this.remaining,this.travelLog,this.mrX,this.detectives,this.moves,this.mrxTurn,this.played);
					//have to make this.remaining played and others empty

				}
				else{
					//logic
					//update to travel log
					Player changingMrX;
					if (move instanceof Move.DoubleMove) {
						roundNum+=1;

						Iterator<Ticket> iteratorX = move.tickets().iterator();
						Ticket first = iteratorX.next();
						Ticket second = iteratorX.next();
						ImmutableList<LogEntry> newTravelLog = ImmutableList.<LogEntry>builder()
								.addAll(this.travelLog)
								.add(this.setup.moves.get(roundNum-1)?LogEntry.reveal(first, destination.get(0)):LogEntry.hidden(first))
								.add(this.setup.moves.get(roundNum)?LogEntry.reveal(second,destination.get(1)):LogEntry.hidden(second))
								.build();

// Update the reference
						this.travelLog = newTravelLog;
						changingMrX=mrX.use(first);
						changingMrX=changingMrX.use(second);
						changingMrX=changingMrX.at(destination.get(1));
						changingMrX=changingMrX.use(Ticket.DOUBLE);

					}
					else{

						Iterator<Ticket> iteratorX = move.tickets().iterator();
						Ticket first = iteratorX.next();
						ImmutableList<LogEntry> newTravelLog = ImmutableList.<LogEntry>builder()
								.addAll(this.travelLog)
								.add(this.setup.moves.get(roundNum)?LogEntry.reveal(first, destination.get(0)):LogEntry.hidden(first))

								.build();
						this.travelLog = newTravelLog;
						changingMrX=mrX.use(first);
						changingMrX=changingMrX.at(destination.get(0));

					}
					this.mrX=changingMrX;
					this.mrxTurn=false;
					return new MyGameState(this.setup,this.remaining,this.travelLog,this.mrX,this.detectives,null,this.mrxTurn,this.played);

				}




				 // Return new state with move applied
			}

			@Nonnull
			@Override
			public GameSetup getSetup() {
				return setup;
			}

			@Nonnull
			@Override
			public ImmutableSet<Piece> getPlayers() {
				ImmutableSet.Builder<Piece> builder = ImmutableSet.builder();
				builder.add(mrX.piece());
				for (Player detective : detectives) {
					builder.add(detective.piece());
				}
				return builder.build();
			}

			@Nonnull
			@Override
			public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
				for (Player realdetective : detectives) {
					if (realdetective.piece().isDetective() &&
							detective.webColour().equals(realdetective.piece().webColour())) {
						return Optional.of(realdetective.location());
					}
				}
				return Optional.empty();
			}

			@Nonnull
			@Override
			public Optional<TicketBoard> getPlayerTickets(Piece piece) {

				if (piece.equals(mrX.piece())) {
					return Optional.of(new TicketBoard() {
						public int getCount(@Nonnull Ticket ticket) {
							return mrX.tickets().getOrDefault(ticket, 0);
						}
					});
				}

				for (Player detective : detectives) {
					if (detective.piece().equals(piece)) {
						return Optional.of(new TicketBoard() {
							public int getCount(@Nonnull Ticket ticket) {
								return detective.tickets().getOrDefault(ticket, 0);
							}
						});
					}
				}
				return Optional.empty();
			}

			@Nonnull
			@Override
			public ImmutableList<LogEntry> getMrXTravelLog() {

				return travelLog;
			}

			@Nonnull
			@Override
			public ImmutableSet<Piece> getWinner() {


				if (this.mrxTurn){
					ImmutableSet<Move> MrXMoves=this.getAvailableMoves();

					this.mrxTurn=false;
					this.moves=null;
					ImmutableSet<Move> detectivemoves=this.getAvailableMoves();
					this.remaining=ImmutableSet.of();
					this.mrxTurn=true;
					this.moves=MrXMoves;

					if ( roundNum==setup.moves.size()){
						return ImmutableSet.of(mrX.piece());
					} else if (detectivemoves.isEmpty()) {
						return ImmutableSet.of(mrX.piece());

					}else if(this.moves.isEmpty()){

						return detectives.stream()
								.map(obj -> obj.piece())
								.collect(ImmutableSet.toImmutableSet());
					}else {
						for (Player p:this.detectives){
							if(p.location()==mrX.location()){
								return detectives.stream()
										.map(obj -> obj.piece())
										.collect(ImmutableSet.toImmutableSet());
							}

						}
						return ImmutableSet.of();

					}
				}else{
					if (this.remaining.isEmpty()&&this.played.isEmpty()){
						return ImmutableSet.of(mrX.piece());
					}else {

						for (Player p:this.detectives){
							if(p.location()==mrX.location()){
								return detectives.stream()
										.map(obj -> obj.piece())
										.collect(ImmutableSet.toImmutableSet());
							}

						}
						return ImmutableSet.of();
					}
				}



			}

			@Nonnull
			@Override
			public ImmutableSet<Move> getAvailableMoves() {
				if (this.moves!=null){
					return this.moves;
				}
				Set<DoubleMove> doubleMoves;
				Set<SingleMove> singleMoves=new HashSet<>();// Declare outside
				if (mrxTurn) {
					doubleMoves = makeDoubleMoves(this.setup, this.detectives, mrX, mrX.location());
					singleMoves=makeSingleMoves(this.setup,this.detectives,mrX,mrX.location());
				} else {
					doubleMoves = ImmutableSet.of();
					Set<SingleMove> eachDetectiveMove;
					ImmutableSet.Builder<Piece> newRemaining= ImmutableSet.<Piece>builder();// or Collections.emptySet()

					for (Player p:this.detectives){
						eachDetectiveMove=makeSingleMoves(this.setup,this.detectives,p,p.location());
						if(!eachDetectiveMove.isEmpty() && !this.played.contains(p.piece())){
							newRemaining.add(p.piece());
							singleMoves.addAll(eachDetectiveMove);

						}
											}
                    this.remaining= newRemaining.build();



				}


				return ImmutableSet.<Move>builder()
						.addAll(doubleMoves)
						.addAll(singleMoves)
						.build();
			}
			private Set<DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source){
				Set<DoubleMove> moves = new HashSet<>();
				Set<SingleMove> singlemoves=makeSingleMoves(setup,detectives,player,source);
				Optional<TicketBoard> tickets=getPlayerTickets(player.piece());
				if (!tickets.isPresent()){
					return Set.of();//imutable empty set
				}
				TicketBoard ticketsActual=tickets.get();

				if(ticketsActual.getCount(Ticket.DOUBLE)<1||setup.moves.size()<2){
					return Set.of();
				}
				for (SingleMove move1:singlemoves){
					int newSource=move1.destination;
					for(int destination : setup.graph.adjacentNodes(newSource)) {
						if (!detectiveLocations.contains(destination)) {
							for (Transport t : setup.graph.edgeValueOrDefault(newSource, destination, ImmutableSet.of())) {

								if (t.requiredTicket()==move1.ticket&&ticketsActual.getCount(t.requiredTicket())>=2||t.requiredTicket()!=move1.ticket&&ticketsActual.getCount(t.requiredTicket())>=1&&ticketsActual.getCount(move1.ticket)>=1){
									moves.add(new DoubleMove(player.piece(), source,move1.ticket,newSource,t.requiredTicket(),destination));
									//can i make my second move secret
									if (move1.ticket==Ticket.SECRET&&t.requiredTicket()!=Ticket.SECRET&&ticketsActual.getCount(Ticket.SECRET)>=2){

											moves.add(new DoubleMove(player.piece(), source,Ticket.SECRET,newSource,Ticket.SECRET,destination));

									}else if(move1.ticket!=Ticket.SECRET&&t.requiredTicket()!=Ticket.SECRET&&ticketsActual.getCount(Ticket.SECRET)>=1){
										moves.add(new DoubleMove(player.piece(), source,move1.ticket,newSource,Ticket.SECRET,destination));
									}

								}else{
									//can i make my second move secret
									if (move1.ticket==Ticket.SECRET&&t.requiredTicket()!=Ticket.SECRET&&ticketsActual.getCount(Ticket.SECRET)>=2){

										moves.add(new DoubleMove(player.piece(), source,Ticket.SECRET,newSource,Ticket.SECRET,destination));

									}else if(move1.ticket!=Ticket.SECRET&&t.requiredTicket()!=Ticket.SECRET&&ticketsActual.getCount(Ticket.SECRET)>=1){
										moves.add(new DoubleMove(player.piece(), source,move1.ticket,newSource,Ticket.SECRET,destination));
									}
								}
							}
						}
					}
				}
				return moves;
			}
			private  Set<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){
				Optional<TicketBoard> tickets=getPlayerTickets(player.piece());
				if (!tickets.isPresent()){
					return Set.of();//imutable empty set
				}
				TicketBoard ticketsActual=tickets.get();

				Set<SingleMove> moves = new HashSet<>();
				for(int destination : setup.graph.adjacentNodes(source)) {

					if (!detectiveLocations.contains(destination)) {
						for (Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {

							if (ticketsActual.getCount(t.requiredTicket())>=1){
								moves.add(new SingleMove(player.piece(), source,t.requiredTicket(),destination));
								if (t.requiredTicket()!=Ticket.SECRET &&player.isMrX()&&ticketsActual.getCount(Ticket.SECRET)>=1){
									moves.add(new SingleMove(player.piece(), source,Ticket.SECRET,destination));
								}
							}else{
								if(ticketsActual.getCount(Ticket.SECRET)>=1){
									//add makes sure not already there
									moves.add(new SingleMove(player.piece(), source,Ticket.SECRET,destination));
								}
							}
						}


					}
				}

				return moves;
			}
		}
	}


