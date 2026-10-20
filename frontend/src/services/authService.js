import {
  createUserWithEmailAndPassword,
  GoogleAuthProvider,
  sendEmailVerification,
  sendPasswordResetEmail,
  signInWithEmailAndPassword,
  signInWithPopup,
  signOut,
  updateProfile,
} from 'firebase/auth'
import { auth, firebaseConfigured } from '../firebase/firebase'

function requireAuth() {
  if (!firebaseConfigured || !auth) {
    throw new Error('Firebase is not configured. Copy frontend/.env.example to frontend/.env and add your Firebase web app values.')
  }
  return auth
}

export async function registerWithEmail({ email, password, displayName }) {
  const a = requireAuth()
  const cred = await createUserWithEmailAndPassword(a, email, password)
  if (displayName) await updateProfile(cred.user, { displayName })
  await sendEmailVerification(cred.user)
  return cred.user
}

export const loginWithEmail = ({ email, password }) =>
  signInWithEmailAndPassword(requireAuth(), email, password).then((c) => c.user)

export const loginWithGoogle = () => signInWithPopup(requireAuth(), new GoogleAuthProvider()).then((c) => c.user)

export const resetPassword = (email) => sendPasswordResetEmail(requireAuth(), email)

export const resendVerification = () => {
  const user = requireAuth().currentUser
  if (!user) throw new Error('You need to be signed in.')
  return sendEmailVerification(user)
}

export const logout = () => signOut(requireAuth())

/** Friendly text for Firebase error codes. */
export function authErrorMessage(error) {
  const map = {
    'auth/invalid-credential': 'Incorrect email or password.',
    'auth/wrong-password': 'Incorrect email or password.',
    'auth/user-not-found': 'No account found for that email.',
    'auth/email-already-in-use': 'An account with that email already exists.',
    'auth/weak-password': 'Choose a stronger password (at least 6 characters).',
    'auth/invalid-email': 'That email address looks invalid.',
    'auth/too-many-requests': 'Too many attempts. Please wait a bit and try again.',
    'auth/popup-closed-by-user': 'Sign-in was cancelled.',
    'auth/network-request-failed': 'Network error. Check your connection.',
    'auth/unauthorized-domain': 'This domain is not authorised in Firebase. Add it under Authentication → Settings → Authorized domains.',
  }
  return map[error?.code] || error?.message || 'Authentication failed.'
}
