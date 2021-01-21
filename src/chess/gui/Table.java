package chess.gui;

import chess.engine.FEN.FenUtilities;
import chess.engine.board.Board;
import chess.engine.board.BoardUtils;
import chess.engine.board.Move;
import chess.engine.pieces.Piece;
import chess.engine.player.MoveTransition;
import chess.engine.player.ArtificialIntelligence.MiniMax;
import com.google.common.collect.Lists;

import javax.imageio.ImageIO;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;

import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JMenu;
import javax.swing.SwingWorker;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.JProgressBar;
import javax.swing.JDialog;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.BorderLayout;
import java.awt.Point;

import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.Toolkit;
import java.awt.Cursor;
import java.awt.Image;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Thread.sleep;
import static javax.swing.SwingUtilities.isLeftMouseButton;
import static chess.engine.board.Move.*;

public final class Table {

    private final BoardPanel boardPanel;
    private Board chessBoard;
    private final MoveLog moveLog;
    private final GameSetup gameSetup;

    private boolean highlightLegalMoves, showAIThinking, AIThinking, mouseEnteredHighlightMoves;
    private final GameHistoryPanel gameHistoryPanel;
    private final TakenPiecePanel takenPiecePanel;

    private int mousePressedX, mousePressedY;

    private Piece humanMovePiece;

    private static final Dimension OUTER_FRAME_DIMENSION = new Dimension(720, 600);
    private static final Dimension BOARD_PANEL_DIMENSION = new Dimension(400, 400);
    private static final Dimension TILE_PANEL_DIMENSION = new Dimension(10, 10);

    private static final Cursor WAIT_CURSOR = new Cursor(Cursor.WAIT_CURSOR);
    private static final Cursor MOVE_CURSOR = new Cursor(Cursor.MOVE_CURSOR);

    private final PropertyChangeSupport propertyChangeSupport;
    private final JFrame gameFrame;

    private Color darkTileColor, lightTileColor;
    private Color legalMovesLightTileColor, legalMovesDarkTileColor;
    private BoardDirection boardDirection;

    private boolean gameEnded;

    private Table() {
        this.gameFrame = new JFrame("Simple Chess");
        this.gameFrame.setResizable(false);
        this.gameFrame.setLayout(new BorderLayout());
        this.chessBoard = Board.createStandardBoard();
        this.gameHistoryPanel = new GameHistoryPanel();
        this.takenPiecePanel = new TakenPiecePanel();
        this.boardPanel = new BoardPanel();
        this.moveLog = new MoveLog();
        this.gameSetup = new GameSetup(this.gameFrame, true);
        this.highlightLegalMoves = true;
        this.gameFrame.add(this.boardPanel, BorderLayout.CENTER);
        final JMenuBar tableMenuBar = createTableMenuBar();
        this.gameFrame.add(this.takenPiecePanel, BorderLayout.WEST);
        this.gameFrame.add(this.gameHistoryPanel, BorderLayout.EAST);
        this.gameFrame.setJMenuBar(tableMenuBar);
        this.gameFrame.setSize(OUTER_FRAME_DIMENSION);
        this.gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.gameFrame.setLocationRelativeTo(null);
        this.gameFrame.setResizable(false);
        this.gameFrame.setVisible(true);
        this.AIThinking = false;
        this.showAIThinking = true;
        this.mouseEnteredHighlightMoves = true;
        this.gameFrame.setCursor(MOVE_CURSOR);
        this.boardDirection = BoardDirection.NORMAL;
        //a property change listener for AI, as Observable is deprecated
        final PropertyChangeListener propertyChangeListener = propertyChangeEvent -> {
            if (Table.this.getGameSetup().isAIPlayer(Table.this.getGameBoard().currentPlayer()) &&
                    !Table.this.getGameBoard().currentPlayer().isInCheckmate() &&
                    !Table.this.getGameBoard().currentPlayer().isInStalemate()) {
                new AIThinkTank(this).execute();
            }
            this.displayEndGameMessage();
        };

        this.propertyChangeSupport = new PropertyChangeSupport(propertyChangeListener);
        this.propertyChangeSupport.addPropertyChangeListener(propertyChangeListener);

        this.legalMovesLightTileColor = new Color(169, 169, 169);
        this.legalMovesDarkTileColor = new Color(105, 105, 105);

        this.lightTileColor = new Color(240, 217, 181);
        this.darkTileColor = new Color(181, 136, 99);
    }


    //setter
    private void setAIThinking(final boolean AIThinking) { this.AIThinking = AIThinking; }

    private void setMouseEnteredHighlightMoves(final boolean mouseEnteredHighlightMoves) { this.mouseEnteredHighlightMoves = mouseEnteredHighlightMoves; }

    private void setShowAIThinking(final boolean showAIThinking) { this.showAIThinking = showAIThinking; }

    private void enableHighLightMoves(final boolean highlightLegalMoves) { this.highlightLegalMoves = highlightLegalMoves; }

    private void updateGameBoard(final Board board) { this.chessBoard = board; }

    private void setBoardDirection(final BoardDirection boardDirection) { this.boardDirection = boardDirection; }

    //getter
    private boolean getAIThinking() { return this.AIThinking; }

    private boolean getMouseEnteredHighlightMoves() { return this.mouseEnteredHighlightMoves; }

    private boolean getShowAIThinking() { return this.showAIThinking; }

    private boolean getHighLightMovesEnabled() { return this.highlightLegalMoves; }

    private boolean isGameEnded() { return this.gameEnded; }

    private GameSetup getGameSetup() { return this.gameSetup; }

    private Board getGameBoard() { return this.chessBoard; }

    private JFrame getGameFrame() { return this.gameFrame; }

    private BoardPanel getBoardPanel() { return this.boardPanel; }

    private MoveLog getMoveLog() { return this.moveLog; }

    private GameHistoryPanel getGameHistoryPanel() { return this.gameHistoryPanel; }

    private TakenPiecePanel getTakenPiecesPanel() { return this.takenPiecePanel; }

    private BoardDirection getBoardDirection() { return this.boardDirection; }

    //singleton
    public static Table getSingletonInstance() { return SingleTon.INSTANCE; }

    private static final class SingleTon { private static final Table INSTANCE = new Table();}

    private void displayEndGameMessage() {
        if (this.getGameBoard().currentPlayer().isInCheckmate()) {
            JOptionPane.showMessageDialog(this.getBoardPanel(),
                    "Game Over: Player " + this.getGameBoard().currentPlayer() + " is in checkmate!", "Game Over",
                    JOptionPane.INFORMATION_MESSAGE);
            this.gameEnded = true;
        }

        if (this.getGameBoard().currentPlayer().isInStalemate()) {
            JOptionPane.showMessageDialog(this.getBoardPanel(),
                    "Game Over: Player " + this.getGameBoard().currentPlayer() + " is in stalemate!", "Game Over",
                    JOptionPane.INFORMATION_MESSAGE);
            this.gameEnded = true;
        }

        if (this.gameEnded) {
            JOptionPane.showMessageDialog(this.getBoardPanel(),
                    "From Game Menu\n1. New Game to start a new game\n2. Exit Game to exit this game", "Game Over",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }


    public void start() {
        this.getMoveLog().clear();
        this.getGameHistoryPanel().redo(this.getGameBoard(), this.getMoveLog());
        this.getTakenPiecesPanel().redo(this.getMoveLog());
        this.getBoardPanel().drawBoard(this.getGameBoard());
    }

    private static final class AIThinkTank extends SwingWorker<Move, Integer> {

        private final JDialog dialog;
        private final JProgressBar bar;
        private final int max;
        private final Table table;
        private final boolean showProgressbar;

        private AIThinkTank(final Table table) {
            this.table = table;
            this.showProgressbar = this.table.getShowAIThinking();
            if (this.showProgressbar) {
                this.dialog = new JDialog();
                this.dialog.setTitle("AI Thinking...");
                this.bar = new JProgressBar(0, 100);
                this.bar.setStringPainted(true);
                this.bar.setForeground(new Color(50, 205, 50));
                this.dialog.add(this.bar);
                this.dialog.setSize(300, 60);
                this.dialog.setLocationRelativeTo(this.table.getGameFrame());
                this.dialog.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                this.dialog.setVisible(true);
                this.dialog.setResizable(false);
                this.max = this.table.getGameBoard().currentPlayer().getLegalMoves().size();
            } else {
                this.dialog = null;
                this.bar = null;
                this.max = 0;
            }
            this.table.setAIThinking(true);
        }

        @Override
        protected void process(final List<Integer> chunks) { this.bar.setValue(chunks.get(chunks.size() - 1)); }

        @Override
        protected Move doInBackground(){
            try {
                final AtomicBoolean running = new AtomicBoolean(true);
                final MiniMax miniMax = new MiniMax(this.table.getGameSetup().getSearchDepth());

                if (this.showProgressbar) {

                    //progress is shown based on move count / total available moves ratio
                    final Thread displayProgress = new Thread(() -> {
                        this.table.getBoardPanel().updateBoardPanelCursor(Table.WAIT_CURSOR);
                        this.dialog.setCursor(Table.WAIT_CURSOR);
                        while(running.get()) {
                            final double progress = ((double)miniMax.getMoveCount() / this.max) * 100;
                            this.publish((int)progress);//publish the progress
                        }
                        //so 100% progress is shown
                        try {
                            sleep(100);
                            this.dialog.dispose();
                            this.table.getBoardPanel().updateBoardPanelCursor(Table.MOVE_CURSOR);
                        }
                        catch (final InterruptedException e) { e.printStackTrace(); }
                    });
                    displayProgress.start();
                }

                final Move bestMove = miniMax.execute(this.table.getGameBoard());
                //stop the loop in thread
                running.lazySet(false);
                return bestMove;

            } catch (final Exception e) { e.printStackTrace(); }
            return null;
        }

        @Override
        public void done() {
            try {
                final Move bestMove = this.get();
                this.table.updateGameBoard(this.table.getGameBoard().currentPlayer().makeMove(bestMove).getLatestBoard());
                this.table.getMoveLog().addMove(bestMove);
                this.table.getGameHistoryPanel().redo(this.table.getGameBoard(), this.table.getMoveLog());
                this.table.getTakenPiecesPanel().redo(this.table.getMoveLog());
                this.table.getBoardPanel().drawBoard(this.table.getGameBoard());
                this.table.moveMadeUpdate();
                this.table.setAIThinking(false);
            } catch (final ExecutionException | InterruptedException e) { e.printStackTrace(); }
        }
    }

    private void moveMadeUpdate() { this.propertyChangeSupport.firePropertyChange("moveMadeUpdate", PlayerType.COMPUTER, PlayerType.HUMAN); }

    private void setupUpdate(final GameSetup gameSetup) { this.propertyChangeSupport.firePropertyChange("setupUpdate", null, gameSetup); }

    private void undoLastMove() {
        final Move lastMove = this.getMoveLog().removeMove();
        this.updateGameBoard(this.getGameBoard().currentPlayer().undoMove(lastMove).getPreviousBoard());
        this.getGameHistoryPanel().redo(this.getGameBoard(), this.getMoveLog());
        this.getTakenPiecesPanel().redo(this.getMoveLog());
        this.getBoardPanel().drawBoard(this.getGameBoard());
    }

    private JMenu createPreferencesMenu() {
        final JMenu preferenceMenu = new JMenu("Preferences");

        preferenceMenu.addSeparator();

        final JCheckBoxMenuItem legalMoveHighlighterCheckBox = new JCheckBoxMenuItem("Highlight Legal Moves");
        final JCheckBoxMenuItem AIThinkingProgressBarCheckBox = new JCheckBoxMenuItem("Show AI thinking");

        legalMoveHighlighterCheckBox.setState(true);
        AIThinkingProgressBarCheckBox.setState(true);

        legalMoveHighlighterCheckBox.addActionListener(actionEvent -> this.enableHighLightMoves(legalMoveHighlighterCheckBox.isSelected()));
        AIThinkingProgressBarCheckBox.addActionListener(actionEvent -> this.setShowAIThinking(AIThinkingProgressBarCheckBox.isSelected()));

        preferenceMenu.add(legalMoveHighlighterCheckBox);
        preferenceMenu.add(AIThinkingProgressBarCheckBox);

        return preferenceMenu;
    }

    private JMenu createOptionMenu() {

        final JMenu optionMenu = new JMenu("Options");

        final JMenuItem setupGameMenuItem = new JMenuItem("Setup Game");
        setupGameMenuItem.addActionListener(actionEvent -> {
            this.getGameSetup().promptUser();
            this.setupUpdate(this.getGameSetup());
        });

        final JMenuItem undoMoveMenuItem = new JMenuItem("Undo last move");
        undoMoveMenuItem.addActionListener(e -> {
            if(this.getMoveLog().size() > 0) { undoLastMove(); }
        });

        final JMenuItem flipBoardMenuItem = new JMenuItem("Flip board");

        flipBoardMenuItem.addActionListener(e -> {
            this.setBoardDirection(this.getBoardDirection().opposite());
            this.getBoardPanel().drawBoard(this.getGameBoard());
        });

        optionMenu.add(undoMoveMenuItem);
        optionMenu.add(setupGameMenuItem);
        optionMenu.add(flipBoardMenuItem);

        return optionMenu;
    }

    private JMenuBar createTableMenuBar() {
        final JMenuBar tableMenuBar = new JMenuBar();
        tableMenuBar.add(createFileMenu());
        tableMenuBar.add(createPreferencesMenu());
        tableMenuBar.add(createOptionMenu());
        tableMenuBar.add(createBoardColorMenu());
        return tableMenuBar;
    }

    private void restartGame() {
        this.updateGameBoard(Board.createStandardBoard());
        this.getGameHistoryPanel().redo(this.getGameBoard(), this.getMoveLog());
        this.start();
    }

    private JMenu createBoardColorMenu() {
        final JMenu gameMenu = new JMenu("Board Color");

        final JMenuItem blueWhiteMenuItem = new JMenuItem("Blue & White");
        blueWhiteMenuItem.addActionListener(e -> {
            this.darkTileColor = new Color(29 ,61 ,99);
            this.lightTileColor = Color.white;
            //light gray
            this.legalMovesLightTileColor = new Color(169, 169, 169);
            //dark gray
            this.legalMovesDarkTileColor = new Color(105, 105, 105);
            this.getBoardPanel().drawBoard(this.getGameBoard());
        });

        final JMenuItem greyWhiteMenuItem = new JMenuItem("Grey & White");
        greyWhiteMenuItem.addActionListener(e -> {
            this.darkTileColor = new Color(105, 105, 105);
            this.lightTileColor = Color.white;
            //light yellow
            this.legalMovesLightTileColor = new Color(255, 253, 156);
            //yellow
            this.legalMovesDarkTileColor = new Color(255, 252, 84);
            this.getBoardPanel().drawBoard(this.getGameBoard());
        });

        final JMenuItem classicMenuItem = new JMenuItem("Classic Board");
        classicMenuItem.addActionListener(e -> {
            this.lightTileColor = new Color(240, 217, 181);
            this.darkTileColor = new Color(181, 136, 99);
            //light gray
            this.legalMovesLightTileColor = new Color(169, 169, 169);
            //dark gray
            this.legalMovesDarkTileColor = new Color(105, 105, 105);
            this.getBoardPanel().drawBoard(this.getGameBoard());
        });

        gameMenu.add(blueWhiteMenuItem);
        gameMenu.add(classicMenuItem);
        gameMenu.add(greyWhiteMenuItem);

        return gameMenu;
    }

    private JMenu createFileMenu() {
        final JMenu gameMenu = new JMenu("Game");

        final JMenuItem newGameMenuItem = new JMenuItem("New Game");
        newGameMenuItem.addActionListener(e -> {
            this.AIThinking = false;
            if (!this.isGameEnded()) {
                final int confirmedRestart = JOptionPane.showConfirmDialog(Table.this.getBoardPanel(), "Are you sure you want to restart game without saving?", "Restart Game", JOptionPane.YES_NO_CANCEL_OPTION);
                if (confirmedRestart == JOptionPane.NO_OPTION) {
                    FenUtilities.writeFENToFile(this.getGameBoard());
                }
                if (confirmedRestart != JOptionPane.CANCEL_OPTION) {
                    this.gameEnded = false;
                    restartGame();
                }
            }
        });

        final JMenuItem saveGameMenuItem = new JMenuItem("Save Game");
        saveGameMenuItem.addActionListener(e -> {
            final int confirmSave = JOptionPane.showConfirmDialog(Table.this.getBoardPanel(), "Confirm to save this game?", "Save Game", JOptionPane.YES_NO_CANCEL_OPTION);
            if (confirmSave == JOptionPane.YES_OPTION) {
                FenUtilities.writeFENToFile(this.getGameBoard());
                JOptionPane.showMessageDialog(this.getGameFrame(), "Game Saved!");
            }
        });


        final JMenuItem loadGameMenuItem = new JMenuItem("Load Saved Game");
        loadGameMenuItem.addActionListener(e -> {
            final int confirmLoadGame = JOptionPane.showConfirmDialog(Table.this.getBoardPanel(), "Confirm to load previous game?", "Load Game", JOptionPane.YES_NO_CANCEL_OPTION);

            if(confirmLoadGame == JOptionPane.YES_OPTION) {

                this.updateGameBoard(FenUtilities.createGameFromFEN());
                this.getBoardPanel().drawBoard(this.getGameBoard());

                if (this.getGameBoard().currentPlayer().getLeague().isBlack()) {
                    JOptionPane.showMessageDialog(Table.this.getBoardPanel(), "Black to move", "Welcome", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(Table.this.getBoardPanel(), "White to move", "Welcome", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });


        final JMenuItem exitMenuItem = new JMenuItem("Exit game");
        exitMenuItem.addActionListener(actionEvent -> {
            final int confirmedExit = JOptionPane.showConfirmDialog(Table.this.getBoardPanel(), "Are you sure you want to quit game without saving?","Exit Game", JOptionPane.YES_NO_CANCEL_OPTION);
            if (confirmedExit == JOptionPane.YES_OPTION) {
                System.exit(0);
            } else if (confirmedExit == JOptionPane.NO_OPTION) {
                FenUtilities.writeFENToFile(this.getGameBoard());
            }
        });

        gameMenu.add(newGameMenuItem);
        gameMenu.add(saveGameMenuItem);
        gameMenu.add(loadGameMenuItem);
        gameMenu.add(exitMenuItem);

        return gameMenu;
    }

    private final class BoardPanel extends JPanel {

        final List<TilePanel> boardTiles;

        private BoardPanel() {
            super(new GridLayout(8, 8));
            this.boardTiles = new ArrayList<>();
            for (int i = 0; i < BoardUtils.NUM_TILES; i++) {
                final TilePanel tilePanel = new TilePanel(this, i);
                this.boardTiles.add(tilePanel);
            }
            setPreferredSize(BOARD_PANEL_DIMENSION);
            this.validate();
        }

        public List<TilePanel> getBoardTiles() { return Collections.unmodifiableList(this.boardTiles); }
        public void drawBoard(final Board board) {
            this.removeAll();
            for (final TilePanel tilePanel : boardDirection.traverse(boardTiles)) {
                tilePanel.drawTile(board);
                this.add(tilePanel);
            }
            this.validate();
            this.repaint();
        }
        public void updateBoardPanelCursor(final Cursor cursor) { this.setCursor(cursor); }
    }

    public static final class MoveLog {

        private final List<Move> moves;

        public MoveLog() { this.moves = new ArrayList<>(); }

        public List<Move> getMoves() { return this.moves; }

        public void addMove(final Move move) { this.moves.add(move); }

        public int size() { return this.moves.size(); }

        public void clear() { this.moves.clear(); }

        public Move removeMove() { return this.moves.remove(this.moves.size() - 1); }
    }

    enum PlayerType {HUMAN, COMPUTER}

    enum BoardDirection {
        NORMAL {
            @Override
            List<TilePanel> traverse(final List<TilePanel> boardTiles) {
                return boardTiles;
            }

            @Override
            BoardDirection opposite() {
                return FLIPPED;
            }
        },
        FLIPPED {
            @Override
            List<TilePanel> traverse(final List<TilePanel> boardTiles) {
                return Lists.reverse(boardTiles);
            }

            @Override
            BoardDirection opposite() {
                return NORMAL;
            }
        };

        abstract List<TilePanel> traverse(final List<TilePanel> boardTiles);
        abstract BoardDirection opposite();

    }

    private final class TilePanel extends JPanel {

        private final int tileID;
        private final BoardPanel boardPanel;
        private TilePanel(final BoardPanel boardPanel, final int tileID) {
            super(new GridBagLayout());
            this.tileID = tileID;
            this.boardPanel = boardPanel;
            this.setPreferredSize(TILE_PANEL_DIMENSION);
            this.validate();

            this.addMouseListener(new MouseListener() {
                @Deprecated
                public void mouseClicked(final MouseEvent mouseEvent) {}
                @Override
                public void mouseEntered(final MouseEvent mouseEvent) {
                    if (!Table.this.getAIThinking() && Table.this.getMouseEnteredHighlightMoves()) {
                        Table.this.humanMovePiece = Table.this.getGameBoard().getTile(TilePanel.this.tileID).getPiece();
                        highlightLegals(Table.this.getGameBoard());
                    }
                }
                @Override
                public void mouseExited(final MouseEvent mouseEvent) {
                    if (!Table.this.getAIThinking() && Table.this.getMouseEnteredHighlightMoves() && Table.this.humanMovePiece != null) {
                        Table.this.humanMovePiece = null;
                        Table.this.getBoardPanel().drawBoard(Table.this.getGameBoard());
                    }
                }

                @Override
                public void mousePressed(final MouseEvent mouseEvent) {
                    if (Table.this.isGameEnded() || Table.this.getAIThinking()) { return; }
                    if (isLeftMouseButton(mouseEvent)) {
                        if (Table.this.humanMovePiece != null && Table.this.humanMovePiece.getLeague() == Table.this.getGameBoard().currentPlayer().getLeague()) {

                            Table.this.mousePressedY = (int) Math.ceil(mouseEvent.getY() / 64.0);
                            Table.this.mousePressedX = (int) Math.ceil(mouseEvent.getX() / 64.0);

                            Table.this.setMouseEnteredHighlightMoves(false);
                            Table.this.humanMovePiece = Table.this.getGameBoard().getTile(TilePanel.this.tileID).getPiece();
                            final Image image = ((ImageIcon)((JLabel) TilePanel.this.boardPanel.getBoardTiles().get(TilePanel.this.tileID).getComponent(0)).getIcon()).getImage();
                            try {
                                TilePanel.this.boardPanel.updateBoardPanelCursor(Toolkit.getDefaultToolkit().createCustomCursor(image, new Point(mouseEvent.getX(), mouseEvent.getY()), null));
                                TilePanel.this.boardPanel.getBoardTiles().get(TilePanel.this.tileID).remove(0);
                            } catch (final IndexOutOfBoundsException ignored) {}
                            validate();
                            repaint();
                        }
                    }
                }

                @Override
                public void mouseReleased(final MouseEvent mouseEvent) {
                    if (!Table.this.getAIThinking() && isLeftMouseButton(mouseEvent)) {

                        if (Table.this.humanMovePiece != null && Table.this.humanMovePiece.getLeague() == Table.this.getGameBoard().currentPlayer().getLeague()) {

                            final int releasedX = (int) Math.ceil(mouseEvent.getX() / 64.0) - Table.this.mousePressedX;
                            final int releasedY = ((int) Math.ceil(mouseEvent.getY() / 64.0) - Table.this.mousePressedY) * 8;

                            final int direction;

                            if (Table.this.getBoardDirection() == BoardDirection.NORMAL) {
                                direction = 1;
                            } else {
                                direction = -1;
                            }

                            final Move move = MoveFactory.createMove(Table.this.getGameBoard(), Table.this.humanMovePiece, TilePanel.this.tileID,
                                    TilePanel.this.tileID + (direction)*(releasedY + releasedX));
                            final MoveTransition transition = Table.this.getGameBoard().currentPlayer().makeMove(move);
                            if (transition.getMoveStatus().isDone()) {

                                Table.this.updateGameBoard(transition.getLatestBoard());
                                if (move instanceof PawnPromotion) {
                                    TilePanel.this.boardPanel.updateBoardPanelCursor(Table.MOVE_CURSOR);
                                    //display pawn promotion interface
                                    Table.this.updateGameBoard(((PawnPromotion)move).promotePawn(Table.this.getGameBoard()));
                                }
                                Table.this.getMoveLog().addMove(move);
                            }
                            TilePanel.this.boardPanel.drawBoard(Table.this.getGameBoard());
                            SwingUtilities.invokeLater(() -> {

                                Table.this.getGameHistoryPanel().redo(Table.this.getGameBoard(), Table.this.getMoveLog());
                                Table.this.getTakenPiecesPanel().redo(Table.this.getMoveLog());

                                if (!Table.this.getGameSetup().isAIPlayer(Table.this.getGameBoard().currentPlayer())) {
                                    Table.this.displayEndGameMessage();
                                }
                                else if (Table.this.getGameSetup().isAIPlayer(Table.this.getGameBoard().currentPlayer())) {
                                    Table.this.moveMadeUpdate();
                                }
                            });
                        }
                        TilePanel.this.boardPanel.updateBoardPanelCursor(Table.MOVE_CURSOR);
                        Table.this.humanMovePiece = null;
                    }
                    Table.this.setMouseEnteredHighlightMoves(true);
                }
            });
        }

        private void drawTile(final Board board) {
            this.assignTileColor();
            if (board.currentPlayer().isInCheck()) {
                this.highlightKingCheck(board.currentPlayer().getPlayerKing().getPiecePosition());
            }
            this.assignTilePieceIcon(board);
            this.validate();
            this.repaint();
        }

        private void assignTilePieceIcon(final Board board) {
            this.removeAll();
            if (board.getTile(this.tileID).isTileOccupied()) {
                final File imageFile = new File("image/chessPieceImages/");
                final String absolutePieceIconPath = imageFile.getAbsolutePath() + "/";
                final String alliance = board.getTile(this.tileID).getPiece().getLeague().toString().substring(0, 1);
                final String pieceName = board.getTile(this.tileID).getPiece().toString() + ".png";
                try {
                    this.add(new JLabel(new ImageIcon(ImageIO.read(new File(absolutePieceIconPath + alliance + pieceName)))));
                } catch (final IOException e) {
                    System.err.println(absolutePieceIconPath + alliance + pieceName);
                }
            }
        }

        private void highlightKingCheck(final int kingCoordinate) {
            if (kingCoordinate == this.tileID) {
                this.setBackground(Color.RED);
            }
        }

        private void highlightLegals(final Board board) {

            if (Table.this.getHighLightMovesEnabled()) {

                final List<Move> legalMoves = new ArrayList<>(pieceLegalMoves(board));

                for (final Move move : legalMoves) {

                    final int coordinate = move.getDestinationCoordinate();
                    Color tileColor;
                    if (move.isAttack() || move instanceof PawnPromotion && ((PawnPromotion)move).getDecoratedMove().isAttack()) {
                        //dark red
                        this.boardPanel.getBoardTiles().get(coordinate).setBackground(new Color(204, 0, 0));
                    } else {
                        if (BoardUtils.FIRST_ROW.get(coordinate) ||
                                BoardUtils.THIRD_ROW.get(coordinate) ||
                                BoardUtils.FIFTH_ROW.get(coordinate) ||
                                BoardUtils.SEVENTH_ROW.get(coordinate)) {
                            tileColor = (coordinate % 2 == 0 ? Table.this.legalMovesLightTileColor : Table.this.legalMovesDarkTileColor);
                        } else {
                            tileColor = (coordinate % 2 != 0 ? Table.this.legalMovesLightTileColor : Table.this.legalMovesDarkTileColor);
                        }
                        this.boardPanel.getBoardTiles().get(coordinate).setBackground(tileColor);
                    }
                }
            }
        }

        private Collection<Move> pieceLegalMoves(final Board board) {
            if (Table.this.humanMovePiece != null && Table.this.humanMovePiece.getLeague() == board.currentPlayer().getLeague()){
                return Table.this.humanMovePiece.calculateLegalMoves(board);
            }
            return Collections.emptyList();
        }

        private void assignTileColor() {
            if (BoardUtils.FIRST_ROW.get(this.tileID) ||
                    BoardUtils.THIRD_ROW.get(this.tileID) ||
                    BoardUtils.FIFTH_ROW.get(this.tileID) ||
                    BoardUtils.SEVENTH_ROW.get(this.tileID)) {
                this.setBackground(this.tileID % 2 == 0 ? Table.this.lightTileColor : Table.this.darkTileColor);
            }
            else {
                this.setBackground(this.tileID % 2 != 0 ? Table.this.lightTileColor : Table.this.darkTileColor);
            }
        }
    }
}