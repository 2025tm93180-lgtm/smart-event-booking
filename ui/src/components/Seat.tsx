import React from 'react'

type SeatProps = {
  row: string;
  number: number;
  isOccupied: boolean;
  isSelected: boolean;
  onToggle: (seatId: string) => void;
}

export function Seat({ row, number, isOccupied, isSelected, onToggle }: SeatProps) {
  const seatId = `${row}${number}`
  return (
    <button
      disabled={isOccupied}
      title={isOccupied ? `${seatId} unavailable` : `Select seat ${seatId}`}
      onClick={() => onToggle(seatId)}
      className={`seat ${isOccupied ? 'occupied' : ''} ${isSelected ? 'chosen' : ''}`}
    >
      {number}
    </button>
  )
}
