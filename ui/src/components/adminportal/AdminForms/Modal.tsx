import {X} from "lucide-react";

interface ModalProps {
    title: string;
    kicker: string;
    onClose: () => void;
    children: React.ReactNode;
}

export function Modal({ title, kicker, onClose, children }: ModalProps) {
    return (
        <div
            className="admin-modal-backdrop"
            onMouseDown={(e) => {
                if (e.target === e.currentTarget) {
                    onClose();
                }
            }}
        >
            <div className="admin-modal" role="dialog" aria-modal="true">
                <div className="admin-modal-header">
                    <div>
                        <span className="admin-kicker">{kicker}</span>
                        <h2>{title}</h2>
                    </div>
                    <button
                        className="admin-icon-button"
                        onClick={onClose}
                        aria-label="Close form"
                    >
                        <X size={19} />
                    </button>
                </div>
                {children}
            </div>
        </div>
    );
}