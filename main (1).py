import cv2
import numpy as np
import mediapipe as mp

# ============================================
# MEDIAPIPE INITIALIZATION
# ============================================

mp_face_mesh = mp.solutions.face_mesh

face_mesh = mp_face_mesh.FaceMesh(
    refine_landmarks=True,
    max_num_faces=1,
    min_detection_confidence=0.5,
    min_tracking_confidence=0.5
)

mp_selfie_segmentation = mp.solutions.selfie_segmentation

selfie_segmentation = mp_selfie_segmentation.SelfieSegmentation(
    model_selection=1
)

# ============================================
# LANDMARKS
# ============================================

LEFT_EYE = [33, 133, 160, 159, 158, 153]
RIGHT_EYE = [362, 385, 387, 386, 374, 373]

# ============================================
# TEMPORAL SMOOTHING
# ============================================

previous_mask = None

# ============================================
# JAUNDICE FILTER
# ============================================

def apply_jaundice(frame, segmentation_mask, landmarks=None):

    global previous_mask

    h, w, _ = frame.shape

    # =====================================
    # PERSON MASK (FLOAT MASK)
    # =====================================

    person_mask = segmentation_mask.astype(np.float32)

    # smooth segmentation edges
    person_mask = cv2.GaussianBlur(person_mask, (21, 21), 0)

    # =====================================
    # HSV SKIN DETECTION
    # =====================================

    hsv = cv2.cvtColor(frame, cv2.COLOR_BGR2HSV)

    lower_skin = np.array([0, 15, 60], dtype=np.uint8)
    upper_skin = np.array([35, 255, 255], dtype=np.uint8)

    skin_mask = cv2.inRange(hsv, lower_skin, upper_skin)

    # convert binary mask -> float
    skin_mask = skin_mask.astype(np.float32) / 255.0

    # =====================================
    # CLEAN SKIN MASK
    # =====================================

    kernel = cv2.getStructuringElement(
        cv2.MORPH_ELLIPSE,
        (7, 7)
    )

    skin_mask = cv2.erode(skin_mask, kernel, iterations=1)
    skin_mask = cv2.dilate(skin_mask, kernel, iterations=1)

    skin_mask = cv2.GaussianBlur(skin_mask, (11, 11), 0)

    # =====================================
    # COMBINE MASKS (SOFT COMBINATION)
    # =====================================

    combined_mask = person_mask * skin_mask

    combined_mask = np.clip(combined_mask, 0, 1)

    # =====================================
    # TEMPORAL SMOOTHING
    # =====================================

    if previous_mask is None:
        previous_mask = combined_mask.copy()

    combined_mask = cv2.addWeighted(
        combined_mask,
        0.7,
        previous_mask,
        0.3,
        0
    )

    previous_mask = combined_mask.copy()

    # =====================================
    # REALISTIC JAUNDICE COLOR SHIFT
    # =====================================

    hsv_tinted = cv2.cvtColor(frame, cv2.COLOR_BGR2HSV).astype(np.float32)

    # slight hue shift toward yellow
    hsv_tinted[:, :, 0] += 8

    # slightly increase saturation
    hsv_tinted[:, :, 1] *= 1.5

    # slightly darken
    hsv_tinted[:, :, 2] *= 0.92

    hsv_tinted = np.clip(hsv_tinted, 0, 255).astype(np.uint8)

    tinted = cv2.cvtColor(hsv_tinted, cv2.COLOR_HSV2BGR)

    # =====================================
    # APPLY FILTER USING FLOAT MASK
    # =====================================

    combined_mask_3ch = cv2.merge([
        combined_mask,
        combined_mask,
        combined_mask
    ])

    result = (
        frame.astype(np.float32) * (1 - combined_mask_3ch)
        +
        tinted.astype(np.float32) * combined_mask_3ch
    )

    result = np.clip(result, 0, 255).astype(np.uint8)

    # =====================================
    # EXTRA EYE YELLOWING
    # =====================================

    if landmarks is not None:

        eye_overlay = result.copy()

        for idx in LEFT_EYE + RIGHT_EYE:

            x = int(landmarks[idx].x * w)
            y = int(landmarks[idx].y * h)

            # cv2.circle(
            #     eye_overlay,
            #     (x, y),
            #     3,
            #     (0, 255, 255),
            #     -1
            # )

        result = cv2.addWeighted(
            result,
            0.85,
            eye_overlay,
            0.15,
            0
        )

    # =====================================
    # DEBUG WINDOW (OPTIONAL)
    # =====================================

    debug_mask = (combined_mask * 255).astype(np.uint8)

    cv2.imshow("Skin Mask", debug_mask)

    return result

# ============================================
# EDEMA FILTER
# ============================================

def apply_edema(frame, landmarks):

    h, w, _ = frame.shape

    for eye in [LEFT_EYE, RIGHT_EYE]:

        pts = []

        for idx in eye:

            x = int(landmarks[idx].x * w)
            y = int(landmarks[idx].y * h)

            pts.append([x, y])

        pts = np.array(pts, np.int32)

        x, y, bw, bh = cv2.boundingRect(pts)

        x -= 10
        y -= 10
        bw += 20
        bh += 20

        # SAFE CLAMPING
        x = max(0, x)
        y = max(0, y)

        x2 = min(w, x + bw)
        y2 = min(h, y + bh)

        roi = frame[y:y2, x:x2]

        if roi.size == 0:
            continue

        blurred = cv2.GaussianBlur(
            roi,
            (35, 35),
            30
        )

        frame[y:y2, x:x2] = cv2.addWeighted(
            roi,
            0.3,
            blurred,
            0.7,
            0
        )

    return frame

# ============================================
# MAIN
# ============================================

def main():

    print("Choose filter:")
    print("1 -> Jaundice")
    print("2 -> Edema")

    choice = input("Enter 1 or 2: ")

    cap = cv2.VideoCapture(0)

    # HIGHER CAMERA RESOLUTION
    cap.set(cv2.CAP_PROP_FRAME_WIDTH, 1280)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 720)

    while True:

        ret, frame = cap.read()

        if not ret:
            break

        # mirror webcam
        frame = cv2.flip(frame, 1)

        rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)

        # =====================================
        # FACE MESH
        # =====================================

        face_result = face_mesh.process(rgb)

        # =====================================
        # SELFIE SEGMENTATION
        # =====================================

        segmentation_result = selfie_segmentation.process(rgb)

        segmentation_mask = segmentation_result.segmentation_mask

        # =====================================
        # APPLY FILTERS
        # =====================================

        landmarks = None

        if face_result.multi_face_landmarks:

            landmarks = (
                face_result
                .multi_face_landmarks[0]
                .landmark
            )

        if choice == "1":

            frame = apply_jaundice(
                frame,
                segmentation_mask,
                landmarks
            )

        elif choice == "2":

            if landmarks is not None:

                frame = apply_edema(
                    frame,
                    landmarks
                )

        # =====================================
        # DISPLAY
        # =====================================

        cv2.imshow(
            "AR Medical Filter",
            frame
        )

        # PRESS Q TO EXIT
        if cv2.waitKey(1) & 0xFF == ord('q'):
            break

    cap.release()

    cv2.destroyAllWindows()

# ============================================
# ENTRY POINT
# ============================================

if __name__ == "__main__":
    main()