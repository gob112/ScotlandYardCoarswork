package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import jakarta.annotation.Nonnull;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.*;


class NewGameState implements NewGameStateInterface{
    public GameSetup setup;

    public GameSetup getSetup() {
        return this.setup;
    }

    public Player mrX;
    public List<Player> detectives;
    public ImmutableList<LogEntry> travelLog;

    private ImmutableSet<Piece> currentWinners;
    private ImmutableSet<Piece> remaining = ImmutableSet.of();
    public ImmutableSet<Move> moves;

    private ImmutableSet<Piece> remainingdummy = ImmutableSet.of();
    private boolean mrxTurn;
    public boolean isMrxTurn() {
        return this.mrxTurn;
    }
    public Set<Integer> detectiveLocations=new HashSet<>();
    public static int roundNum=1;
    public ImmutableSet<Piece> played;
    public static List<LogEntry> revealedtravelLog=new ArrayList<>();
    private int minMaxRoundNum;
    public static void UpdateRevealedTravelLog(Move move){
        Iterator<ScotlandYard.Ticket> ticketIter = move.tickets().iterator();
        List<Integer> destination = move.accept(new Move.Visitor<>() {
            public List<Integer> visit(Move.SingleMove s) { return List.of(s.destination); }
            public List<Integer> visit(Move.DoubleMove d) { return List.of(d.destination1, d.destination2); }
        });
        if (move instanceof Move.DoubleMove) {
            ScotlandYard.Ticket t1 = ticketIter.next();
            ScotlandYard.Ticket t2 = ticketIter.next();
            revealedtravelLog.add(LogEntry.reveal(t1, destination.get(0)));
            revealedtravelLog.add(LogEntry.reveal(t2, destination.get(1)));



        } else {
            ScotlandYard.Ticket t1 = ticketIter.next();
            revealedtravelLog.add(LogEntry.reveal(t1, destination.get(0)));
        }



    }
    public NewGameState(
            GameSetup setup,
            List<Player> dete,
            Player mrX,
            ImmutableList<LogEntry> travelLog,
            ImmutableSet<Move> moves,final boolean mrxturn,final ImmutableSet<Piece> played,int roundnum,ImmutableSet<Piece> remaining

    ){

        this.detectives=dete;
        this.mrX = mrX;
        this.travelLog = travelLog;
        this.moves = null;

        this.mrxTurn = mrxturn;
        this.played = played;
        this.setup=setup;


        this.minMaxRoundNum=roundnum;
        this.remaining=remaining;

        for (Player detective : this.detectives) {
            this.detectiveLocations.add(detective.location());
        }
        this.moves=availableMoves();
        this.currentWinners=winner();
        if (!this.currentWinners.isEmpty()){
            this.moves=ImmutableSet.of();
        }
    }
    public NewGameState advanceState(Move move) {
        ImmutableSet<Move> legalMoves = statelessAvailableMoves(this.played);
        if (!legalMoves.contains(move)) {
            throw new IllegalArgumentException("Illegal move: " + move);
        }

        // 1. get destination
        List<Integer> destination = move.accept(new Move.Visitor<>() {
            public List<Integer> visit(Move.SingleMove s) { return List.of(s.destination); }
            public List<Integer> visit(Move.DoubleMove d) { return List.of(d.destination1, d.destination2); }
        });

        // 2. copy current state attributes
        List<Player> newD = new ArrayList<>(this.detectives);
        Player newMrX = this.mrX;
        ImmutableList<LogEntry> newLog = this.travelLog;
        ImmutableSet<Piece> newPlayed = this.played;
        boolean newMrXTurn = this.mrxTurn;
        int newRoundNum = this.minMaxRoundNum;
        Set<Piece> remaining=this.remaining;


        // 3. apply move depending on who is moving
        if (!mrxTurn) {
            // A detective is moving
            Piece mover = move.commencedBy();
            if (newPlayed.contains(mover)) {
                throw new IllegalArgumentException("Detective already moved this round: " + mover);
            }

            // Find the detective
            int idx = -1;
            Player det = null;
            for (int i = 0; i < newD.size(); i++) {
                if (newD.get(i).piece().webColour().equals(mover.webColour())) {
                    idx = i;
                    det = newD.get(i);
                    break;
                }
            }
            if (det == null) {
                throw new IllegalArgumentException("Move commenced by unknown player: " + mover);
            }
            if (!newPlayed.contains(det.piece())) {
                Player newDet = det.at(destination.get(0));
                Iterable<ScotlandYard.Ticket> ticketsUsed = move.tickets();
                newMrX = newMrX.give(ticketsUsed);
                newDet = newDet.use(ticketsUsed);

                newD.set(idx, newDet);
                newPlayed = ImmutableSet.<Piece>builder()
                        .addAll(newPlayed)
                        .add(mover)
                        .build();
            }else{
                throw new IllegalArgumentException("player is moving twice");
            }
            // Update detective
            Player finalDet = det;
            ImmutableSet<Piece> newRemaining = this.remaining.stream()
                    .filter(piece -> !piece.equals(finalDet.piece()))
                    .collect(ImmutableSet.toImmutableSet());
            this.moves=null;
            List<Player> oldDet=this.detectives;
            this.detectives=newD;
            ImmutableSet<Piece> oldrem=this.remaining;
            ImmutableSet<Piece> oldPlayed=this.played;
            this.played=newPlayed;
            ImmutableSet<Move> newmoves=this.availableMoves();
            newRemaining=this.remaining;//TODO: this may breakk the code need to check
            this.detectives=oldDet;
            this.remaining=oldrem;
            this.played=oldPlayed;
            if(!newmoves.isEmpty()){



            }
            else{

                newmoves=null;

            }


            if (newRemaining.isEmpty()) {
                newmoves=null;
                newMrXTurn = true;
                newPlayed = ImmutableSet.of();
                newRoundNum++;


            }
            return new NewGameState(this.setup,newD,newMrX,newLog,newmoves,newMrXTurn,newPlayed,newRoundNum,newRemaining);
        }
        else {
            // MrX is moving
            Iterator<ScotlandYard.Ticket> ticketIter = move.tickets().iterator();
            if (move instanceof Move.DoubleMove) {
                ScotlandYard.Ticket t1 = ticketIter.next();
                ScotlandYard.Ticket t2 = ticketIter.next();
                boolean reveal1 = newRoundNum - 1 >= setup.moves.size() || setup.moves.get(newRoundNum - 1);
                boolean reveal2 = newRoundNum >= setup.moves.size() || setup.moves.get(newRoundNum);
                newLog = ImmutableList.<LogEntry>builder()
                        .addAll(newLog)
                        .add(reveal1 ? LogEntry.reveal(t1, destination.get(0)) : LogEntry.hidden(t1))
                        .add(reveal2 ? LogEntry.reveal(t2, destination.get(1)) : LogEntry.hidden(t2))
                        .build();
                newMrX = newMrX.use(t1).use(t2).at(destination.get(1)).use(ScotlandYard.Ticket.DOUBLE).at(destination.get(1));
                newRoundNum += 1;
            } else {
                ScotlandYard.Ticket t1 = ticketIter.next();
                boolean revealThis = newRoundNum >= setup.moves.size() || setup.moves.get(newRoundNum);
                newLog = ImmutableList.<LogEntry>builder()
                        .addAll(newLog)
                        .add(revealThis ? LogEntry.reveal(t1, destination.get(0)) : LogEntry.hidden(t1))
                        .build();
                newMrX = newMrX.use(t1).at(destination.get(0));
            }

        }

        // 4. compute new moves
        ImmutableSet<Move> newMoves =null;  // can recompute later in availableMoves


        return new NewGameState(this.setup, newD, newMrX, newLog, null, newMrXTurn, newPlayed,newRoundNum,this.remaining);
    }



    public Optional<Board.TicketBoard> playerTickets(Piece piece) {

        if (piece.equals(mrX.piece())) {
            return Optional.of(new Board.TicketBoard() {
                public int getCount(@Nonnull ScotlandYard.Ticket ticket) {
                    return mrX.tickets().getOrDefault(ticket, 0);
                }
            });
        }

        for (Player detective : detectives) {
            if (detective.piece().equals(piece)) {
                return Optional.of(new Board.TicketBoard() {
                    public int getCount(@Nonnull ScotlandYard.Ticket ticket) {
                        return detective.tickets().getOrDefault(ticket, 0);
                    }
                });
            }
        }
        return Optional.empty();
    }
    public ImmutableSet<Move> availableMoves() {

        if (this.moves != null) {
            return this.moves;
        }

        Set<Move> moves = new HashSet<>();

        if (mrxTurn) {
            moves.addAll(makeSingleMoves(setup, detectives, mrX, mrX.location()));
            moves.addAll(makeDoubleMoves(setup, detectives, mrX, mrX.location()));
        } else {
            ImmutableSet.Builder<Piece> newRemaining= ImmutableSet.<Piece>builder();
            for (Player p : detectives) {
                if (!played.contains(p.piece())) {
                    moves.addAll(makeSingleMoves(setup, detectives, p, p.location()));
                    newRemaining.add(p.piece());
                }
            }
            this.remaining=newRemaining.build();
        }

        this.moves = ImmutableSet.copyOf(moves);
        return this.moves;
    }
    //TODO:stateless as well
    public ImmutableSet<Move> filteredAvailableMoves(ImmutableSet<Piece> newPlayed ,DistanceMainCalculator distancematrix) {


        Set<Move> moves = new HashSet<>();

        if (mrxTurn) {
            moves.addAll(makeSingleMoves(setup, detectives, mrX, mrX.location()));
            moves.addAll(makeDoubleMoves(setup, detectives, mrX, mrX.location()));
        } else {
            ImmutableSet.Builder<Piece> newRemaining= ImmutableSet.<Piece>builder();
            for (Player p : detectives) {
                if (!newPlayed.contains(p.piece())) {
                    if (distancematrix.getdistance(p.location(),this.mrX.location())>2){
                        int destin=distancematrix.getNextBestMove(p.location(),this.mrX.location());
                        moves.addAll(makeSingleMovesFiltered(setup,detectives,p,p.location(),destin));

                    }else {
                        moves.addAll(makeSingleMovesFiltered(setup, detectives, p, p.location(),-1));
                    }

                    newRemaining.add(p.piece());
                }
            }
            this.remainingdummy=newRemaining.build();
        }

         return ImmutableSet.copyOf(moves);  // freeze once

    }
    public ImmutableSet<Move> statelessAvailableMoves(ImmutableSet<Piece> newPlayed ) {



        Set<Move> moves = new HashSet<>();

        if (mrxTurn) {
          moves.addAll(makeDoubleMovesFiltered(setup, detectives, mrX, mrX.location()));
                    moves.addAll(makeSingleMoves(setup, detectives, mrX, mrX.location()));
                    moves.addAll(makeDoubleMoves(setup, detectives, mrX, mrX.location()));
        } else {
            ImmutableSet.Builder<Piece> newRemaining= ImmutableSet.<Piece>builder();
            for (Player p : detectives) {
                if (!newPlayed.contains(p.piece())) {
                    moves.addAll(makeSingleMoves(setup, detectives, p, p.location()));
                    newRemaining.add(p.piece());
                }
            }
            this.remainingdummy=newRemaining.build();
        }

        return ImmutableSet.copyOf(moves);

    }
    private Set<Move.DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source){
        List<Integer> detectiveLocations = this.detectives.stream().map(p->p.location()).toList();
        Set<Move.DoubleMove> moves = new HashSet<>();
        Set<Move.SingleMove> singlemoves=makeSingleMoves(setup,detectives,player,source);
        Optional<Board.TicketBoard> tickets=playerTickets(player.piece());
        if (!tickets.isPresent()){
            return Set.of();
        }
        Board.TicketBoard ticketsActual=tickets.get();

        if(ticketsActual.getCount(ScotlandYard.Ticket.DOUBLE)<1||setup.moves.size()<2){
            return Set.of();
        }
        for (Move.SingleMove move1:singlemoves){
            int newSource=move1.destination;
            for(int destination : setup.graph.adjacentNodes(newSource)) {
                if (!detectiveLocations.contains(destination)) {
                    for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(newSource, destination, ImmutableSet.of())) {

                        if (t.requiredTicket()==move1.ticket&&ticketsActual.getCount(t.requiredTicket())>=2||t.requiredTicket()!=move1.ticket&&ticketsActual.getCount(t.requiredTicket())>=1&&ticketsActual.getCount(move1.ticket)>=1){
                            moves.add(new Move.DoubleMove(player.piece(), source,move1.ticket,newSource,t.requiredTicket(),destination));

                            if (move1.ticket== ScotlandYard.Ticket.SECRET&&t.requiredTicket()!= ScotlandYard.Ticket.SECRET&&ticketsActual.getCount(ScotlandYard.Ticket.SECRET)>=2){

                                moves.add(new Move.DoubleMove(player.piece(), source, ScotlandYard.Ticket.SECRET,newSource, ScotlandYard.Ticket.SECRET,destination));

                            }else if(move1.ticket!= ScotlandYard.Ticket.SECRET&&t.requiredTicket()!= ScotlandYard.Ticket.SECRET&&ticketsActual.getCount(ScotlandYard.Ticket.SECRET)>=1){
                                moves.add(new Move.DoubleMove(player.piece(), source,move1.ticket,newSource, ScotlandYard.Ticket.SECRET,destination));
                            }

                        }else{

                            if (move1.ticket== ScotlandYard.Ticket.SECRET&&t.requiredTicket()!= ScotlandYard.Ticket.SECRET&&ticketsActual.getCount(ScotlandYard.Ticket.SECRET)>=2){

                                moves.add(new Move.DoubleMove(player.piece(), source, ScotlandYard.Ticket.SECRET,newSource, ScotlandYard.Ticket.SECRET,destination));

                            }else if(move1.ticket!= ScotlandYard.Ticket.SECRET&&t.requiredTicket()!= ScotlandYard.Ticket.SECRET&&ticketsActual.getCount(ScotlandYard.Ticket.SECRET)>=1){
                                moves.add(new Move.DoubleMove(player.piece(), source,move1.ticket,newSource, ScotlandYard.Ticket.SECRET,destination));
                            }
                        }
                    }
                }
            }
        }
        return moves;
    }
    private Set<Move.DoubleMove> makeDoubleMovesFiltered(GameSetup setup, List<Player> detectives, Player player, int source){
        List<Integer> detectiveLocations = this.detectives.stream().map(p->p.location()).toList();
        Set<Move.DoubleMove> moves = new HashSet<>();
        Set<Move.SingleMove> singlemoves=makeSingleMoves(setup,detectives,player,source);
        Optional<Board.TicketBoard> tickets=playerTickets(player.piece());
        if (!tickets.isPresent()){
            return Set.of();
        }
        Board.TicketBoard ticketsActual=tickets.get();

        if(ticketsActual.getCount(ScotlandYard.Ticket.DOUBLE)<1||setup.moves.size()<2){
            return Set.of();
        }
        for (Move.SingleMove move1:singlemoves){
            int newSource=move1.destination;
            for(int destination : setup.graph.adjacentNodes(newSource)) {
                if (!detectiveLocations.contains(destination)) {
                    if (setup.graph.edgeValueOrDefault(source,destination,ImmutableSet.of()).contains(ScotlandYard.Transport.TAXI)){
                        if (ScotlandYard.Ticket.TAXI==move1.ticket&&ticketsActual.getCount(ScotlandYard.Ticket.TAXI)>=2|| ScotlandYard.Ticket.TAXI!=move1.ticket&&ticketsActual.getCount(ScotlandYard.Ticket.TAXI)>=1&&ticketsActual.getCount(move1.ticket)>=1){
                            moves.add(new Move.DoubleMove(player.piece(), source,move1.ticket,newSource, ScotlandYard.Ticket.TAXI,destination));


                            break;
                        }

                    }if (setup.graph.edgeValueOrDefault(source,destination,ImmutableSet.of()).contains(ScotlandYard.Transport.BUS)){
                        if (ScotlandYard.Ticket.BUS==move1.ticket&&ticketsActual.getCount(ScotlandYard.Ticket.BUS)>=2|| ScotlandYard.Ticket.BUS!=move1.ticket&&ticketsActual.getCount(ScotlandYard.Ticket.BUS)>=1&&ticketsActual.getCount(move1.ticket)>=1){
                            moves.add(new Move.DoubleMove(player.piece(), source,move1.ticket,newSource, ScotlandYard.Ticket.BUS,destination));


                            break;
                        }
                    }if (setup.graph.edgeValueOrDefault(source,destination,ImmutableSet.of()).contains(ScotlandYard.Transport.UNDERGROUND)){

                        if (ScotlandYard.Ticket.UNDERGROUND==move1.ticket&&ticketsActual.getCount(ScotlandYard.Ticket.UNDERGROUND)>=2|| ScotlandYard.Ticket.UNDERGROUND!=move1.ticket&&ticketsActual.getCount(ScotlandYard.Ticket.UNDERGROUND)>=1&&ticketsActual.getCount(move1.ticket)>=1){
                            moves.add(new Move.DoubleMove(player.piece(), source,move1.ticket,newSource, ScotlandYard.Ticket.UNDERGROUND,destination));


                            break;
                        }
                    }if (setup.graph.edgeValueOrDefault(source,destination,ImmutableSet.of()).contains(ScotlandYard.Transport.FERRY)){
                        if (ScotlandYard.Ticket.SECRET==move1.ticket&&ticketsActual.getCount(ScotlandYard.Ticket.SECRET)>=2|| ScotlandYard.Ticket.SECRET!=move1.ticket&&ticketsActual.getCount(ScotlandYard.Ticket.SECRET)>=1&&ticketsActual.getCount(move1.ticket)>=1){
                            moves.add(new Move.DoubleMove(player.piece(), source,move1.ticket,newSource, ScotlandYard.Ticket.SECRET,destination));


                            break;
                        }
                    }

                }
            }
        }
        return moves;
    }
    private  Set<Move.SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){
        List<Integer> detectiveLocations = this.detectives.stream().map(p->p.location()).toList();
        Optional<Board.TicketBoard> tickets=playerTickets(player.piece());
        if (!tickets.isPresent()){
            return Set.of();//imutable empty set
        }
        Board.TicketBoard ticketsActual=tickets.get();

        Set<Move.SingleMove> moves = new HashSet<>();
        for(int destination : setup.graph.adjacentNodes(source)) {

            //  if the location is occupied, don't add to the collection of moves to return
            if (!detectiveLocations.contains(destination)) {
                for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {

                    //  if it does, construct a SingleMove and add it the collection of moves to return
                    if (ticketsActual.getCount(t.requiredTicket())>=1){
                        moves.add(new Move.SingleMove(player.piece(), source,t.requiredTicket(),destination));
                        if (t.requiredTicket()!= ScotlandYard.Ticket.SECRET &&player.isMrX()&&ticketsActual.getCount(ScotlandYard.Ticket.SECRET)>=1){
                            moves.add(new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.SECRET,destination));
                        }
                    }else{
                        if(ticketsActual.getCount(ScotlandYard.Ticket.SECRET)>=1){

                            moves.add(new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.SECRET,destination));
                        }
                    }
                }


            }

        }





        return moves;
    }
    private  Set<Move.SingleMove> makeSingleMovesFiltered(GameSetup setup, List<Player> detectives, Player player, int source,int fixedDestin){
        List<Integer> detectiveLocations = this.detectives.stream().map(p->p.location()).toList();
        Optional<Board.TicketBoard> tickets=playerTickets(player.piece());
        if (!tickets.isPresent()){
            return Set.of();//imutable empty set
        }
        Board.TicketBoard ticketsActual=tickets.get();

        Set<Move.SingleMove> moves = new HashSet<>();
        if (fixedDestin>0){
            if (!detectiveLocations.contains(fixedDestin)) {
                if (setup.graph.edgeValueOrDefault(source,fixedDestin,ImmutableSet.of()).contains(ScotlandYard.Transport.TAXI)){
                    if (ticketsActual.getCount(ScotlandYard.Ticket.TAXI)>=1) {
                        moves.add(new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.TAXI, fixedDestin));

                    }

                }if (setup.graph.edgeValueOrDefault(source,fixedDestin,ImmutableSet.of()).contains(ScotlandYard.Transport.BUS)){
                    if (ticketsActual.getCount(ScotlandYard.Ticket.BUS)>=1) {
                        moves.add(new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.BUS, fixedDestin));

                    }
                }if (setup.graph.edgeValueOrDefault(source,fixedDestin,ImmutableSet.of()).contains(ScotlandYard.Transport.UNDERGROUND)){

                    if (ticketsActual.getCount(ScotlandYard.Ticket.UNDERGROUND)>=1) {
                        moves.add(new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.UNDERGROUND, fixedDestin));

                    }
                }if (setup.graph.edgeValueOrDefault(source,fixedDestin,ImmutableSet.of()).contains(ScotlandYard.Transport.FERRY)){
                    if (ticketsActual.getCount(ScotlandYard.Ticket.SECRET)>=1) {
                        moves.add(new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, fixedDestin));

                    }
                }



                //  add moves to the fixedDestin via a secret ticket if there are any left with the player
            }
        }else  {
            for(int destination : setup.graph.adjacentNodes(source)) {

                //  if the location is occupied, don't add to the collection of moves to return
                if (!detectiveLocations.contains(destination)) {
                    if (setup.graph.edgeValueOrDefault(source,destination,ImmutableSet.of()).contains(ScotlandYard.Transport.TAXI)){
                        if (ticketsActual.getCount(ScotlandYard.Ticket.TAXI)>=1) {
                            moves.add(new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.TAXI, destination));
                            break;
                        }

                    }if (setup.graph.edgeValueOrDefault(source,destination,ImmutableSet.of()).contains(ScotlandYard.Transport.BUS)){
                        if (ticketsActual.getCount(ScotlandYard.Ticket.BUS)>=1) {
                            moves.add(new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.BUS, destination));
                            break;
                        }
                    }if (setup.graph.edgeValueOrDefault(source,destination,ImmutableSet.of()).contains(ScotlandYard.Transport.UNDERGROUND)){

                        if (ticketsActual.getCount(ScotlandYard.Ticket.UNDERGROUND)>=1) {
                            moves.add(new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.UNDERGROUND, destination));
                            break;
                        }
                    }if (setup.graph.edgeValueOrDefault(source,destination,ImmutableSet.of()).contains(ScotlandYard.Transport.FERRY)){
                        if (ticketsActual.getCount(ScotlandYard.Ticket.SECRET)>=1) {
                            moves.add(new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination));
                            break;
                        }
                    }



                    //  add moves to the destination via a secret ticket if there are any left with the player
                }
        }


        }





        return moves;
    }
    public ImmutableSet<Piece> winner() {
        // Detectors win if MrX is captured or out of moves
        for (Player p : detectives) {
            if (p.location() == mrX.location()) {
                return detectives.stream()
                        .map(Player::piece)
                        .collect(ImmutableSet.toImmutableSet());
            }
        }

        // Check if it’s MrX turn
        if (mrxTurn) {
            ImmutableSet<Move> mrXMoves = availableMoves();
            if (this.minMaxRoundNum == setup.moves.size()) {
                // Game over, MrX wins
                return ImmutableSet.of(mrX.piece());
            } else if (mrXMoves.isEmpty()) {
                // MrX has no legal moves on his turn
                return detectives.stream()
                        .map(Player::piece)
                        .collect(ImmutableSet.toImmutableSet());
            }
        } else {
            // It’s detective turn
            if (this.minMaxRoundNum == setup.moves.size()) {
                // Game over, MrX wins
                return ImmutableSet.of(mrX.piece());
            }
            if (this.availableMoves().isEmpty() && played.isEmpty()) {
                // All detectives played; MrX wins
                return ImmutableSet.of(mrX.piece());
            }
        }

        // No winner yet
        return ImmutableSet.of();



    }


}




