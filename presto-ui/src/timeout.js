import React, { useEffect, useRef, useState } from "react";
import ReactDOM from "react-dom";

const setupInactivityMonitor = (timeout) => {
  let timer = null;
  let lastActivity = Date.now();

  const handleInactivity = () => {
    console.log('User inactive, redirecting to logout page...'); // For testing
    window.location.href = 'logout.html';
  };

  const resetTimer = () => {
    lastActivity = Date.now();
    if (timer) {
      clearTimeout(timer);
    }
    timer = setTimeout(() => {
      handleInactivity();
    }, timeout);
  };

  const handleActivity = () => {
    resetTimer();
  };

  const handleVisibilityChange = () => {
    const now = Date.now();
    const timeSinceLastActivity = now - lastActivity;

    if (timeSinceLastActivity >= timeout) {
      handleInactivity();
    } else {
      resetTimer();
    }
  };

  // Set up event listeners
  const events = ['mousemove', 'keydown', 'click', 'scroll'];
  events.forEach(event => {
    document.addEventListener(event, handleActivity, true);
  });

  // We need a dedicated handler for tab visibility changes
  document.addEventListener('visibilitychange', handleVisibilityChange);

  // Start the timer
  resetTimer();

  // Cleanup function
  return () => {
    events.forEach(event => {
      document.removeEventListener(event, handleActivity, true);
    });
    document.removeEventListener('visibilitychange', handleVisibilityChange);
    if (timer) {
      clearTimeout(timer);
    }
  };
};

const InactivityMonitor = () => {
  const [timeoutValue, setTimeoutValue] = useState(0);
  const cleanupRef = useRef(null);

  useEffect(() => {
    const fetchTimeoutValue = async (retries = 5) => {
      try {
        const response = await fetch('/v1/ui/timeout');
        if (response.ok) {
          const data = await response.json();
          setTimeoutValue(data.timeout);
        } else {
          throw new Error('Failed to fetch timeout value');
        }
      } catch (error) {
        if (retries > 0) {
          console.warn(`Retrying... Attempts left: ${retries - 1}`);
          fetchTimeoutValue(retries - 1);
        } else {
          console.error('All retries failed. Using default timeout value.');
          setTimeoutValue(900000); //15 min
        }
      }
    };

    fetchTimeoutValue();
  }, []);

  useEffect(() => {
    if (timeoutValue > 0) {
      if (cleanupRef.current) {
        cleanupRef.current();
      }
      cleanupRef.current = setupInactivityMonitor(timeoutValue);
    }
    return () => {
      if (cleanupRef.current) {
        cleanupRef.current();
      }
    };
  }, [timeoutValue]);

  return null;
};

ReactDOM.render(<InactivityMonitor />, document.body.appendChild(document.createElement('div')));
