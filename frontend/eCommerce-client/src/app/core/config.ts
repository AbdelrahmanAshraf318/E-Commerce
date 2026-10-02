/** Backend origin. Swap per environment (or move to src/environments when you add a production build). */
export const API_ORIGIN = 'http://localhost:8080';
export const API_URL = `${API_ORIGIN}/api/v1`;

/** Full-page navigation target that starts Spring Security's Google OAuth2 flow. */
export const GOOGLE_LOGIN_URL = `${API_ORIGIN}/oauth2/authorization/google`;
