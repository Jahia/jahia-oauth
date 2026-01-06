export interface ClientCredentials {
    clientId: string;
    clientSecret: string;
}

export interface GoogleUser {
    sub: string;
    name: string;
    givenName: string;
    familyName: string;
    email: string;
    emailVerified: boolean;
    picture: string;
}

export interface LinkedInUser {
    id: string;
    localizedFirstName: string;
    localizedLastName: string;
    emailAddress: string;
    pictureUrl?: string;
}

export interface GitHubUser {
    id: number;
    login: string;
    name: string;
    email: string;
    avatar_url: string;
    location?: string;
    bio?: string;
}

export interface FacebookUser {
    id: string;
    name: string;
    first_name: string;
    last_name: string;
    email: string;
    picture?: {
        data: {
            url: string;
        };
    };
}

