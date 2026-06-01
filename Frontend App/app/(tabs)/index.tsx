import { Isloggedin } from "@/context/IsLoggedIn";
import { Ionicons } from "@expo/vector-icons";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { router, Stack } from "expo-router";
import { useContext, useEffect, useState } from "react";
import { Image, StyleSheet, Text, TouchableOpacity, View } from "react-native";

export default function HomeScreen() {
  const { isLoggedin, setIsLoggedin } = useContext(Isloggedin);

  const [currentUser, setCurrentUser] = useState<any>(null);

  useEffect(() => {
    const loadUser = async () => {
      const data = await AsyncStorage.getItem("currentUser");
      if (data) {
        setCurrentUser(JSON.parse(data));
      }
    };

    loadUser();
  }, []);


  const handleLogout = () => {
    setIsLoggedin(false);
    router.replace("/");
  };

return (
  <>
    <Stack.Screen
      options={{
        title: "Home",
        headerBackVisible: false,
      }}
    />

    <View style={styles.container}>

      <View style={styles.topBar}>

        {isLoggedin && (
          <TouchableOpacity style={styles.iconBtn} onPress={handleLogout}>
            <Ionicons name="log-out-outline" size={24} color="white" />
          </TouchableOpacity>
        )}


        {isLoggedin ? (
          <Text style={{ color: "white" }}>
            Welcome, {currentUser?.username}
          </Text>
        ) : (
          <View style={{ flex: 1 }} />
        )}


        <TouchableOpacity style={styles.iconBtn} onPress={() => router.push("/Settings")}>
          <Ionicons name="settings-outline" size={24} color="white" />
        </TouchableOpacity>

      </View>

      <View style={styles.center}>
        <Image
          source={require('@/assets/images/logo.png')}
          style={styles.logo}
        />

        <Text style={styles.subtitle}>
          Try realistic face effects
        </Text>
      </View>

      <View style={styles.buttons}>
        {isLoggedin ? (
          <>
            <TouchableOpacity
              style={styles.mainBtn}
              onPress={() => router.push("/Camera")}
            >
              <Text style={{ color: "white" }}>Start Camera</Text>
              <Ionicons name="camera-outline" size={22} color="white" />
            </TouchableOpacity>
          </>
        ) : (
          <>
            <TouchableOpacity
              style={styles.mainBtn}
              onPress={() => router.push("/Login")}
            >
              <Text style={{ color: "white" }}>Login</Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={styles.mainBtn}
              onPress={() => router.push("/Signup")}
            >
              <Text style={{ color: "white" }}>Signup</Text>
            </TouchableOpacity>
          </>
        )}
      </View>

    </View>
  </>
);

}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    gap: 180,
    paddingTop: 20,
    paddingBottom: 40,
    alignItems: "center",
    backgroundColor: "#0F1115"
  },

  topBar: {
    flexDirection: "row",
    width: "100%",
    justifyContent: "space-between",
    alignItems: "center",
    paddingHorizontal: 20,
  },

  iconBtn: {
    backgroundColor: "#6C5CE7",
    padding: 12,
    borderRadius: 10,
  },

  center: {
    alignItems: "center",
  },

  logo: {
    width: 250,
    height: 150,
  },

  subtitle: {
    color: "#beb9b9",
    fontSize: 18,
    marginTop: 20,
  },

  buttons: {
    flexDirection: "row",
    gap: 20,
  },

  mainBtn: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
    backgroundColor: "#6C5CE7",
    padding: 15,
    borderRadius: 12,
  },
});