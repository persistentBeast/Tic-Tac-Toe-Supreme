import React, { useState, useEffect, useRef } from 'react';
import axios from 'axios';
import './App.css';
import clickSound from './click.wav'; // Adjust the path as necessary
import completionSound from './winner.wav'; // Adjust the path as necessary

const Loader = () => {
  return (
    <div className="loader-container">
      <div className="loader"></div>
      <p className="loading-message">Finding players online...</p>
    </div>
  );
};

const App = () => {
  const [loading, setLoading] = useState(false);
  const [username, setUsername] = useState('');
  const [userId, setUserId] = useState(null);
  const [gameId, setGameId] = useState(null);
  const [opponentName, setOpponentName] = useState(null);
  const [symbol, setSymbol] = useState(null);
  const [status, setStatus] = useState(null);
  const [gameState, setGameState] = useState("---------");
  const [message, setMessage] = useState("");
  const [showWinner, setShowWinner] = useState(false);
  const ws = useRef(null);
  const clickAudio = useRef(new Audio(clickSound));
  const completionAudio = useRef(new Audio(completionSound));

  useEffect(() => {
    if (gameId) {
      // Open WebSocket connection
      ws.current = new WebSocket(`${apiUrl}/${gameId}`);

      ws.current.onopen = () => {
        console.log('WebSocket connection opened');
        // Send ACK every 5 seconds
        const ackInterval = setInterval(() => {
          ws.current.send(JSON.stringify({ type: 'ack', user_id: userId, game_id: gameId }));
        }, 5000);

        ws.current.onclose = () => {
          clearInterval(ackInterval);
          console.log('WebSocket connection closed');
        };
      };

      ws.current.onmessage = (event) => {
        const message = JSON.parse(event.data);
        if (message.type === 'state') {
          setGameState(message.state);
          setStatus(message.status);
          if (message.status === 'LOADING') {
            setLoading(true);
            setMessage('Waiting for other player');
          } else if (message.status === 'LIVE') {
            setLoading(false);
            setMessage('');
          } else if (message.status === 'COMPLETED') {
            setLoading(false);
            setMessage(`Game Over. Winner: ${message.winner}`);
            setShowWinner(true);
            completionAudio.current.play();
            ws.current.close();
          }
        }
      };

      return () => {
        if (ws.current) {
          ws.current.close();
        }
      };
    }
  }, [gameId, userId]);

  const startGame = async () => {
    if (!username.trim()) {
      alert('Please enter your username');
      return;
    }
    setLoading(true);
    try {
      const response = await axios.post(`${apiUrl}/api/v1/play`, { user_name: username });
      const data = response.data;
      if (data.status === 'SUCCESS') {
        setUserId(data.user_id);
        setGameId(data.game_id);
        setOpponentName(data.opponent_name);
        setSymbol(data.symbol);
        setStatus(data.status);
      } else {
        setMessage('Failed to start game');
      }
    } catch (error) {
      console.error('Error starting game', error);
      setMessage('Error starting game');
    } finally {
      setLoading(false);
    }
  };

  const handleMove = (index) => {
    if (status === 'LIVE' && gameState[index] === '-') {
      clickAudio.current.play();
      ws.current.send(JSON.stringify({
        type: 'activity',
        user_id: userId,
        game_id: gameId,
        move: index,
        move_id: `${userId}-${index}`
      }));
    }
  };

  const resetGame = () => {
    setGameId(null);
    setGameState("---------");
    setMessage("");
    setShowWinner(false);
  };

  return (
    <div className="app">
      <h1 className="title">Multiplayer Tic-Tac-Toe</h1>
      <div className="input-container">
        <input
          type="text"
          placeholder="Enter your username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          className="username-input"
          disabled={loading}
        />
        <button className="start-btn" onClick={startGame} disabled={loading}>Start Game</button>
      </div>
      {loading && <Loader />}
      {message && <p className="message">{message}</p>}
      {gameId && !loading && (
        <div className="game-container">
          <div className="player-info">
            <p className="info">Your Symbol: {symbol === 1 ? 'X' : 'O'}</p>
            <p className="info">Opponent: {opponentName}</p>
          </div>
          <div className="board">
            {gameState.split('').map((cell, index) => (
              <div key={index} className="cell" onClick={() => handleMove(index)}>
                {cell !== '-' && <span className={cell === '1' ? 'cross' : 'circle'}>{cell === '1' ? 'X' : 'O'}</span>}
              </div>
            ))}
          </div>
        </div>
      )}
      {showWinner && (
        <div className="winner-popup">
          <p>{message}</p>
          <button className="reset-btn" onClick={resetGame}>Start New Game</button>
        </div>
      )}
    </div>
  );
};

export default App;
