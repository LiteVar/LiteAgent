import React from 'react';

const Bg: React.FC = React.memo(() => {
  return (
    <div
      style={{
        position: 'absolute',
        inset: 0,
        background: `
          radial-gradient(ellipse 60% 50% at 85% 5%, rgba(155,200,250,0.65) 0%, transparent 70%),
          radial-gradient(ellipse 80% 70% at 30% 25%, rgba(165,185,220,0.6) 0%, transparent 70%),
          radial-gradient(ellipse 55% 50% at 68% 65%, rgba(168,215,238,0.6) 0%, transparent 70%),
          radial-gradient(ellipse 70% 80% at 85% 85%, rgba(155,200,250,0.6) 0%, transparent 70%),
          radial-gradient(ellipse 65% 75% at 20% 95%, rgba(165,185,220,0.55) 0%, transparent 70%),
          radial-gradient(ellipse 60% 55% at 25% 0%, rgba(190,240,250,0.6) 0%, transparent 70%),
          white
        `,
      }}
    />
  );
});

Bg.displayName = 'Bg';

export default Bg;
