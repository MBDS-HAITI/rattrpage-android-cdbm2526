// src/components/ArrierePlanTraces.jsx
//
// Fond decoratif : quelques traces courbes bleues sur un arriere-plan
// clair, inspire des portails institutionnels. Purement visuel, place
// derriere le contenu (z-index negatif), sans aucune interaction.

function ArrierePlanTraces() {
  return (
    <svg
      className="arriere-plan-traces"
      viewBox="0 0 1440 900"
      preserveAspectRatio="xMidYMid slice"
      aria-hidden="true"
    >
      <path
        d="M -100 700 C 200 600, 400 850, 700 650 S 1100 400, 1540 500"
        fill="none" stroke="#2563eb" strokeWidth="1.4" opacity="0.16"
      />
      <path
        d="M -100 500 C 250 750, 500 550, 800 750 S 1200 850, 1540 700"
        fill="none" stroke="#2563eb" strokeWidth="1.4" opacity="0.13"
      />
      <path
        d="M -100 850 C 300 700, 550 950, 900 780 S 1250 600, 1540 750"
        fill="none" stroke="#2563eb" strokeWidth="1.2" opacity="0.11"
      />
      <path
        d="M 200 -50 C 350 250, 150 450, 400 600 S 700 750, 650 950"
        fill="none" stroke="#2563eb" strokeWidth="1.2" opacity="0.10"
      />
      <path
        d="M 1000 -50 C 900 200, 1150 350, 1000 550 S 800 800, 950 950"
        fill="none" stroke="#2563eb" strokeWidth="1.4" opacity="0.14"
      />
    </svg>
  );
}

export default ArrierePlanTraces;
