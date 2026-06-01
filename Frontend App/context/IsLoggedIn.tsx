import { createContext, useState } from "react";

export const Isloggedin = createContext<any>(null);

export default function IsLoggedInProvider({ children }: any) {
  const [isLoggedin, setIsLoggedin] = useState<boolean>(false);

  return (
    <Isloggedin.Provider value={{ isLoggedin, setIsLoggedin }}>
      {children}
    </Isloggedin.Provider>
  );
}