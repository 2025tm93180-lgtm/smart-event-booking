import React, { useState, useEffect } from 'react';
import { ArrowRight } from 'lucide-react';

const HERO_SECTIONS = [
  {
    eyebrow: 'YOUR NEXT GREAT MEMORY STARTS HERE',
    title: <>Make room for<br/><em>the extraordinary.</em></>,
    description: 'Find the nights, sounds, and moments you’ll talk about long after they’re over.',
    image: 'https://images.unsplash.com/photo-1470229722913-7c0e2dbbafd3?auto=format&fit=crop&w=2200&q=90',
    caption: 'LIVE EXPERIENCES, ALL IN ONE PLACE'
  },
  {
    eyebrow: 'DISCOVER THE BEAT OF THE CITY',
    title: <>Feel the energy<br/><em>of live music.</em></>,
    description: 'From underground clubs to massive stadiums, experience the music that moves you.',
    image: 'https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?auto=format&fit=crop&w=2200&q=90',
    caption: 'ICONIC PERFORMANCES, NIGHTLY'
  },
  {
    eyebrow: 'WITNESS THE GLORY IN PERSON',
    title: <>Be part of<br/><em>sporting history.</em></>,
    description: 'Catch the high-stakes drama and unforgettable plays live from the best seats in the house.',
    image: 'https://images.unsplash.com/photo-1504450758481-7338eba7524a?auto=format&fit=crop&w=2200&q=90',
    caption: 'CHAMPIONSHIP MOMENTS, LIVE'
  }
];

export const Hero: React.FC = () => {
  const [currentIndex, setCurrentIndex] = useState(0);

  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentIndex((prev) => (prev + 1) % HERO_SECTIONS.length);
    }, 3000);
    return () => clearInterval(timer);
  }, []);

  const section = HERO_SECTIONS[currentIndex];

  return (
    <section className="hero">
      <div 
        className="hero-image" 
        style={{ backgroundImage: `url(${section.image})` }} 
      />
      <div className="hero-vignette" />
      <div className="hero-content">
        <div className="eyebrow"><span className="live-dot" /> {section.eyebrow}</div>
        <h1>{section.title}</h1>
        <p>{section.description}</p>
        <div className="hero-actions">
          <button className="button button-primary" onClick={() => document.getElementById('events')?.scrollIntoView({ behavior: 'smooth' })}>Explore experiences <ArrowRight size={17}/></button>
          <div className="social-proof"><div className="avatar-stack"><span>J</span><span>A</span><span>M</span></div><span><strong>12k+</strong> experiences booked</span></div>
        </div>
      </div>
      <div className="hero-index">
        <span>0{currentIndex + 1}</span>
        <span className="index-line"/>
        <span>FEATURED</span>
      </div>
      <div className="hero-caption"><span className="live-dot"/> {section.caption}</div>
      
      <div className="hero-dots">
        {HERO_SECTIONS.map((_, i) => (
          <button 
            key={i} 
            className={`hero-dot ${i === currentIndex ? 'active' : ''}`}
            onClick={() => setCurrentIndex(i)}
            aria-label={`Go to slide ${i + 1}`}
          />
        ))}
      </div>
    </section>
  );
};
