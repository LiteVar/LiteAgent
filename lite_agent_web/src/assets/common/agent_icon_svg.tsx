import React, { useMemo, useId } from 'react';

interface AgentIconSvgProps {
  variantIndex?: number;
  seed?: string;
  width?: number | string;
  height?: number | string;
  className?: string;
}

const VARIANTS = [
  {
    bgEndColor: '#194CCB',
    ellipse5Fill: '#2D43B1',
    ellipse6Fill: '#2D73B1',
    unionPath: 'M33.2217 20.3447L19.9492 33.6885L6.67773 20.3447L19.9492 7L33.2217 20.3447Z',
    unionStops: [
      { offset: 0, color: '#3636DC', opacity: 0.42 },
      { offset: 0.124108, color: '#3678DC' },
      { offset: 0.218549, color: '#9ED6FF' },
      { offset: 0.343649, color: 'white' },
      { offset: 1, color: 'white' },
    ],
  },
  {
    bgEndColor: '#1990CB',
    ellipse5Fill: '#4DD2E7',
    ellipse6Fill: '#137E8F',
    unionPath: 'M33.5549 20.3447L20.2825 33.6885L7.01099 20.3447L20.2825 7L33.5549 20.3447Z',
    unionStops: [
      { offset: 0, color: '#36B3DC', opacity: 0.42 },
      { offset: 0.124108, color: '#36D9DC' },
      { offset: 0.218549, color: '#9EFCFF' },
      { offset: 0.343649, color: 'white' },
      { offset: 1, color: 'white' },
    ],
  },
  {
    bgEndColor: '#391185',
    ellipse5Fill: '#C174F1',
    ellipse6Fill: '#36026D',
    unionPath: 'M32.8882 20.3447L19.6157 33.6885L6.34424 20.3447L19.6157 7L32.8882 20.3447Z',
    unionStops: [
      { offset: 0, color: '#7336DC', opacity: 0.42 },
      { offset: 0.124108, color: '#6836DC' },
      { offset: 0.218549, color: '#B09EFF' },
      { offset: 0.343649, color: 'white' },
      { offset: 1, color: 'white' },
    ],
  },
  {
    bgEndColor: '#0D4700',
    ellipse5Fill: '#9FB12D',
    ellipse6Fill: '#66B12D',
    unionPath: 'M33.2217 20.3447L19.9492 33.6885L6.67773 20.3447L19.9492 7L33.2217 20.3447Z',
    unionStops: [
      { offset: 0, color: '#D6DC36', opacity: 0.42 },
      { offset: 0.124108, color: '#A2DC36' },
      { offset: 0.218549, color: '#EAFF9E' },
      { offset: 0.343649, color: 'white' },
      { offset: 1, color: 'white' },
    ],
  },
  {
    bgEndColor: '#23268D',
    ellipse5Fill: '#D5C7FA',
    ellipse6Fill: '#3C63E8',
    unionPath: 'M33.2217 20.3447L19.9492 33.6885L6.67773 20.3447L19.9492 7L33.2217 20.3447Z',
    unionStops: [
      { offset: 0, color: '#3F408C' },
      { offset: 0.124108, color: '#3654DC' },
      { offset: 0.218549, color: '#9ED5FF' },
      { offset: 0.343649, color: 'white' },
      { offset: 1, color: 'white' },
    ],
  },
];

const AgentIconSvg: React.FC<AgentIconSvgProps> = ({ variantIndex, seed, width = 40, height = 40, className }) => {
  const id = useId();
  const safeId = id.replace(/:/g, '_');

  const variant = useMemo(() => {
    if (variantIndex !== undefined) {
      return VARIANTS[variantIndex % VARIANTS.length];
    }
    let index: number;
    if (seed) {
      let hash = 0;
      for (let i = 0; i < seed.length; i++) {
        hash = seed.charCodeAt(i) + ((hash << 5) - hash);
      }
      index = Math.abs(hash) % VARIANTS.length;
    } else {
      index = Math.floor(Math.random() * VARIANTS.length);
    }
    return VARIANTS[index];
  }, [variantIndex, seed]);

  return (
    <svg width={width} height={height} viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg" className={className}>
      <g clipPath={`url(#clip0_${safeId})`}>
        <rect width="40" height="40" rx="8" fill={`url(#paint0_linear_${safeId})`} />
        <g filter={`url(#filter0_f_${safeId})`}>
          <ellipse cx="-2.49279" cy="4.55584" rx="12.5215" ry="12.6934" fill={variant.ellipse5Fill} />
        </g>
        <g filter={`url(#filter1_f_${safeId})`}>
          <ellipse cx="41.6332" cy="36.9914" rx="12.5215" ry="12.6934" fill={variant.ellipse6Fill} />
        </g>
        <g filter={`url(#filter2_n_${safeId})`}>
          <path d={variant.unionPath} fill={`url(#paint1_linear_${safeId})`} />
        </g>
        <path d="M19.4293 15.6123C19.5133 15.3956 19.8199 15.3956 19.9039 15.6123L21.0226 18.4987C21.0484 18.5654 21.1012 18.6182 21.1679 18.6441L24.0544 19.7627C24.2711 19.8467 24.2711 20.1533 24.0544 20.2373L21.1679 21.3559C21.1012 21.3818 21.0484 21.4346 21.0226 21.5013L19.9039 24.3877C19.8199 24.6044 19.5133 24.6044 19.9039 24.3877L18.3107 21.5013C18.2848 21.4346 18.2321 21.3818 18.1654 21.3559L15.2789 20.2373C15.0622 20.1533 15.0622 19.8467 15.2789 19.7627L18.1654 18.6441C18.2321 18.6182 18.2848 18.5654 18.3107 18.4987L19.4293 15.6123Z" fill="#191925" />
      </g>
      <defs>
        <filter id={`filter0_f_${safeId}`} x="-31.8624" y="-24.9857" width="58.7392" height="59.0831" filterUnits="userSpaceOnUse" colorInterpolationFilters="sRGB">
          <feFlood floodOpacity="0" result="BackgroundImageFix" />
          <feBlend mode="normal" in="SourceGraphic" in2="BackgroundImageFix" result="shape" />
          <feGaussianBlur stdDeviation="8.42407" result="effect1_foregroundBlur" />
        </filter>
        <filter id={`filter1_f_${safeId}`} x="12.2636" y="7.44983" width="58.7392" height="59.0831" filterUnits="userSpaceOnUse" colorInterpolationFilters="sRGB">
          <feFlood floodOpacity="0" result="BackgroundImageFix" />
          <feBlend mode="normal" in="SourceGraphic" in2="BackgroundImageFix" result="shape" />
          <feGaussianBlur stdDeviation="8.42407" result="effect1_foregroundBlur" />
        </filter>
        <filter id={`filter2_n_${safeId}`} x="6.67773" y="7" width="26.5439" height="26.6885" filterUnits="userSpaceOnUse" colorInterpolationFilters="sRGB">
          <feFlood floodOpacity="0" result="BackgroundImageFix" />
          <feBlend mode="normal" in="SourceGraphic" in2="BackgroundImageFix" result="shape" />
          <feTurbulence type="fractalNoise" baseFrequency="29.669313430786133 29.669313430786133" stitchTiles="stitch" numOctaves="3" result="noise" seed="6879" />
          <feColorMatrix in="noise" type="luminanceToAlpha" result="alphaNoise" />
          <feComponentTransfer in="alphaNoise" result="coloredNoise1">
            <feFuncA type="discrete" tableValues="1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 " />
          </feComponentTransfer>
          <feComposite operator="in" in2="shape" in="coloredNoise1" result="noise1Clipped" />
          <feFlood floodColor="rgba(0, 0, 0, 0.25)" result="color1Flood" />
          <feComposite operator="in" in2="noise1Clipped" in="color1Flood" result="color1" />
          <feMerge result="effect1_noise">
            <feMergeNode in="shape" />
            <feMergeNode in="color1" />
          </feMerge>
        </filter>
        <linearGradient id={`paint0_linear_${safeId}`} x1="20" y1="0" x2="20" y2="40" gradientUnits="userSpaceOnUse">
          <stop stopColor="#000000" />
          <stop offset="1" stopColor={variant.bgEndColor} />
        </linearGradient>
        <linearGradient id={`paint1_linear_${safeId}`} x1="32.122" y1="21.3871" x2="9.26098" y2="-13.5524" gradientUnits="userSpaceOnUse">
          {variant.unionStops.map((stop, index) => (
            <stop key={index} offset={stop.offset} stopColor={stop.color} stopOpacity={stop.opacity ?? 1} />
          ))}
        </linearGradient>
        <clipPath id={`clip0_${safeId}`}>
          <rect width="40" height="40" rx="8" fill="white" />
        </clipPath>
      </defs>
    </svg>
  );
};

export default AgentIconSvg;
