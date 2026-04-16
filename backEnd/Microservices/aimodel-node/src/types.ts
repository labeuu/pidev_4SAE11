interface JwtPayloadLike {
  [key: string]: unknown;
}

declare global {
  namespace Express {
    interface Request {
      auth?: JwtPayloadLike;
    }
  }
}

export {};
