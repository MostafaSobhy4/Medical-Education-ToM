import { createContext, useState } from "react";

export const ImageContext = createContext<any>(null);

export default function ImageProvider({ children }: any) {
  const [image, setImage] = useState<string | null>(null);

  return (
    <ImageContext.Provider value={{ image, setImage }}>
      {children}
    </ImageContext.Provider>
  );
}