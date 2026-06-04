import { createContext, useState } from "react";

export const ImageContext = createContext<any>(null);

export default function ImageProvider({ children }: any) {
  const [image, setImage] = useState<string | null>(null);
  const [filter, setFilter] = useState(null);

  return (
    <ImageContext.Provider value={{ image, setImage, filter, setFilter }}>
      {children}
    </ImageContext.Provider>
  );
}