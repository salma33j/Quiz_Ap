import { Link } from 'react-router-dom';
export default function Unauthorized(){return <div style={{padding:40}}><h1>Accès refusé</h1><p>Vous n’avez pas le droit d’accéder à cette page.</p><Link to="/login">Retour connexion</Link></div>}
