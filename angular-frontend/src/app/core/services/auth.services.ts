import { Injectable } from '@angular/core';
import {
  CognitoUserPool, CognitoUser, AuthenticationDetails,
  CognitoUserSession, ISignUpResult
} from 'amazon-cognito-identity-js';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private userPool = new CognitoUserPool({
    UserPoolId: environment.cognitoUserPoolId,
    ClientId: environment.cognitoClientId
  });

  login(email: string, password: string): Promise<CognitoUserSession> {
    const user = new CognitoUser({ Username: email, Pool: this.userPool });
    const auth = new AuthenticationDetails({ Username: email, Password: password });
    return new Promise((resolve, reject) =>
      user.authenticateUser(auth, { onSuccess: resolve, onFailure: reject })
    );
  }

  signUp(email: string, password: string): Promise<ISignUpResult> {
    return new Promise((resolve, reject) =>
      this.userPool.signUp(email, password, [], [], (err, result) =>
        err ? reject(err) : resolve(result!)
      )
    );
  }

  logout(): void {
    this.userPool.getCurrentUser()?.signOut();
  }

  getIdToken(): Promise<string | null> {
    return new Promise(resolve => {
      const user = this.userPool.getCurrentUser();
      if (!user) return resolve(null);
      user.getSession((err: any, session: CognitoUserSession) =>
        resolve(err || !session.isValid() ? null : session.getIdToken().getJwtToken())
      );
    });
  }

  isLoggedIn(): Promise<boolean> {
    return this.getIdToken().then(t => t !== null);
  }
}