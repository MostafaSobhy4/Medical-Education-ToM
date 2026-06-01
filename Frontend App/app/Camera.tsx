import { ImageContext } from "@/context/ImageContext";
import { CameraView, useCameraPermissions } from "expo-camera";
import { router } from "expo-router";
import { useContext, useRef, useState } from "react";
import { Image, StyleSheet, Text, TouchableOpacity, View } from "react-native";

export default function CameraScreen() {
  const cameraRef = useRef<any>(null);
  const { setImage } = useContext(ImageContext);

  const [permission, requestPermission] = useCameraPermissions();

  const [activeFilter, setActiveFilter] = useState<string | null>(null);

  const filters = [
    {
      id: "jaundice",
      image: require("@/assets/images/Icon-Jaundice.png"),
    },
    {
      id: "rash",
      image: require("@/assets/images/Butterfly Rash.png"),
    },
  ];

  if (!permission) return <View />;

  if (!permission.granted) {
    return (
      <View style={styles.permissionContainer}>
        <Text style={styles.permissionText}>We need camera permission</Text>

        <TouchableOpacity onPress={requestPermission} style={styles.button}>
          <Text style={styles.buttonText}>Grant Permission</Text>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <>
      <CameraView
        ref={cameraRef}
        style={{ flex: 1 }}
        facing="front"
      />

      <View style={styles.controls}>
        
        <View style={styles.filtersContainer}>
          {filters.map((filter) => (
            <TouchableOpacity
              key={filter.id}
              onPress={() => setActiveFilter(filter.id)}
            >
              <Image
                source={filter.image}
                style={[
                  styles.filterImage,
                  activeFilter === filter.id && styles.activeFilter,
                ]}
              />
            </TouchableOpacity>
          ))}
        </View>

        <TouchableOpacity
          onPress={async () => {
            const photo = await cameraRef.current.takePictureAsync();

            setImage(photo.uri);

            console.log("Selected filter:", activeFilter);

            router.replace("/Result");
          }}
          style={styles.captureButton}
        >
          <Text style={styles.buttonText}>Capture</Text>
        </TouchableOpacity>
      </View>
    </>
  );
}

const styles = StyleSheet.create({
  permissionContainer: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    backgroundColor: "#0F1115",
  },
  permissionText: {
    color: "white",
    marginBottom: 20,
  },
  controls: {
    position: "absolute",
    bottom: 120,
    alignSelf: "center",
  },
  filtersContainer: {
    flexDirection: "row",
    gap: 30,
    marginBottom: 15,
    justifyContent: "center",
  },
  filterImage: {
    width: 50,
    height: 50,
    borderWidth: 2,
    borderColor: "transparent",
    borderRadius: 50,
  },
  activeFilter: {
    borderColor: "#6C5CE7",
    transform: [{ scale: 1.1 }],
  },
  captureButton: {
    width: 110,
    alignItems: "center",
    justifyContent: "center",
    alignSelf: "center",
    backgroundColor: "#6C5CE7",
    padding: 15,
    borderRadius: 12,
  },
  button: {
    backgroundColor: "#6C5CE7",
    padding: 15,
    borderRadius: 12,
  },
  buttonText: {
    color: "white",
    fontWeight: "bold",
  },
});
